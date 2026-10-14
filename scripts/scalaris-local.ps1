param(
  [ValidateSet('Start', 'Stop', 'Backup', 'Status', 'Access')]
  [string]$Action = 'Start',
  [string]$DataDirectory,
  [switch]$NoBrowser,
  [switch]$NoBuild,
  [switch]$FunctionsOnly
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

function Get-ScalarisPaths([string]$Directory) {
  $repoRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
  if ([string]::IsNullOrWhiteSpace($Directory)) {
    if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
      throw 'No se encontro la carpeta local del usuario de Windows.'
    }
    $Directory = Join-Path $env:LOCALAPPDATA 'ScalarisCRM'
  }
  $dataRoot = [System.IO.Path]::GetFullPath($Directory).TrimEnd('\', '/')
  $protectedRoots = @(
    [System.IO.Path]::GetPathRoot($dataRoot).TrimEnd('\', '/'),
    $env:USERPROFILE, $env:LOCALAPPDATA, $env:APPDATA, $env:ProgramFiles, $env:ProgramData, $env:WINDIR
  )
  foreach ($protectedRoot in $protectedRoots) {
    if ($protectedRoot -and $dataRoot.Equals($protectedRoot.TrimEnd('\', '/'), [System.StringComparison]::OrdinalIgnoreCase)) {
      throw 'Seleccione una carpeta exclusiva para Scalaris CRM, no una carpeta general de Windows.'
    }
  }
  if ($repoRoot.StartsWith($dataRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Seleccione una carpeta exclusiva fuera del proyecto, no su carpeta contenedora.'
  }
  if ($dataRoot.Equals($repoRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
      $dataRoot.StartsWith($repoRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Las credenciales y respaldos deben guardarse fuera de la carpeta del proyecto.'
  }
  return @{
    Repo = $repoRoot
    Data = $dataRoot
    Env = Join-Path $dataRoot 'scalaris.env'
    Access = Join-Path $dataRoot 'Acceso-inicial.txt'
    Backup = Join-Path $dataRoot 'Respaldos'
    Compose = Join-Path $repoRoot 'docker-compose.scalaris.yml'
    Project = 'scalaris-crm-local'
  }
}

function Test-ScalarisPrivateAcl($Acl, [switch]$Directory) {
  $allowed = @([System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value, 'S-1-5-18')
  if (-not $Acl.AreAccessRulesProtected -or @($Acl.Access).Count -ne 2) { return $false }
  $seen = @()
  foreach ($rule in $Acl.Access) {
    $sid = $rule.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value
    if ($allowed -notcontains $sid -or $seen -contains $sid -or $rule.AccessControlType -ne 'Allow' -or
        $rule.FileSystemRights -ne [System.Security.AccessControl.FileSystemRights]::FullControl) { return $false }
    if ($Directory -and $rule.InheritanceFlags -ne [System.Security.AccessControl.InheritanceFlags]'ContainerInherit, ObjectInherit') { return $false }
    $seen += $sid
  }
  return $true
}

function Protect-ScalarisDirectory([string]$Path) {
  New-Item -ItemType Directory -Path $Path -Force | Out-Null
  $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent()
  $acl = Get-Acl -LiteralPath $Path
  if (Test-ScalarisPrivateAcl $acl -Directory) { return }
  $acl.SetAccessRuleProtection($true, $false)
  foreach ($rule in @($acl.Access)) { $null = $acl.RemoveAccessRuleSpecific($rule) }
  $inheritance = [System.Security.AccessControl.InheritanceFlags]'ContainerInherit, ObjectInherit'
  $propagation = [System.Security.AccessControl.PropagationFlags]::None
  foreach ($sid in @($identity.User, [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
    $rule = [System.Security.AccessControl.FileSystemAccessRule]::new(
      $sid, [System.Security.AccessControl.FileSystemRights]::FullControl,
      $inheritance, $propagation, [System.Security.AccessControl.AccessControlType]::Allow
    )
    $acl.AddAccessRule($rule)
  }
  Set-Acl -LiteralPath $Path -AclObject $acl
}

function Protect-ScalarisFile([string]$Path) {
  $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent()
  $acl = Get-Acl -LiteralPath $Path
  if (Test-ScalarisPrivateAcl $acl) { return }
  $acl.SetAccessRuleProtection($true, $false)
  foreach ($rule in @($acl.Access)) { $null = $acl.RemoveAccessRuleSpecific($rule) }
  foreach ($sid in @($identity.User, [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
    $acl.AddAccessRule([System.Security.AccessControl.FileSystemAccessRule]::new(
      $sid, [System.Security.AccessControl.FileSystemRights]::FullControl,
      [System.Security.AccessControl.AccessControlType]::Allow
    ))
  }
  Set-Acl -LiteralPath $Path -AclObject $acl
}

function New-ScalarisSecret {
  $bytes = New-Object byte[] 24
  $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
  try { $random.GetBytes($bytes) } finally { $random.Dispose() }
  return ([System.BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
}

function Read-ScalarisConfiguration([string]$Path) {
  $required = @(
    'SCALARIS_DATABASE_PASSWORD', 'SCALARIS_BOOTSTRAP_USERNAME', 'SCALARIS_BOOTSTRAP_PASSWORD',
    'SCALARIS_MARKETING_USERNAME', 'SCALARIS_MARKETING_PASSWORD', 'SCALARIS_MARKETING_DISPLAY_NAME',
    'SCALARIS_FRONTEND_PORT'
  )
  $values = @{}
  foreach ($rawLine in [System.IO.File]::ReadAllLines($Path)) {
    $line = $rawLine.Trim().TrimStart([char]0xFEFF)
    if ($line.Length -eq 0 -or $line.StartsWith('#')) { continue }
    if ($line -notmatch '^(?<key>SCALARIS_[A-Z_]+)=(?<value>[A-Za-z0-9 ._-]+)$') {
      throw 'La configuracion local tiene un formato invalido. No se modificaron sus credenciales.'
    }
    $key = $matches.key
    $value = $matches.value
    if ($required -notcontains $key -or $values.ContainsKey($key)) {
      throw 'La configuracion local tiene campos desconocidos o repetidos.'
    }
    $values[$key] = $value
  }
  foreach ($key in $required) {
    if (-not $values.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($values[$key])) {
      throw "Falta $key en la configuracion local. No se generaron claves nuevas."
    }
  }
  foreach ($key in @('SCALARIS_DATABASE_PASSWORD', 'SCALARIS_BOOTSTRAP_PASSWORD', 'SCALARIS_MARKETING_PASSWORD')) {
    if ($values[$key].Length -lt 24) { throw 'Una clave local no cumple el largo minimo de 24 caracteres.' }
  }
  foreach ($key in @('SCALARIS_BOOTSTRAP_USERNAME', 'SCALARIS_MARKETING_USERNAME')) {
    if ($values[$key] -notmatch '^[a-zA-Z0-9._-]{3,80}$') { throw 'Un nombre de usuario local tiene un formato invalido.' }
  }
  if ($values['SCALARIS_BOOTSTRAP_USERNAME'] -eq $values['SCALARIS_MARKETING_USERNAME']) {
    throw 'La cuenta de Marketing y la cuenta de administracion deben ser diferentes.'
  }
  $port = 0
  if (-not [int]::TryParse($values['SCALARIS_FRONTEND_PORT'], [ref]$port) -or $port -lt 1024 -or $port -gt 65535) {
    throw 'El puerto local debe estar entre 1024 y 65535.'
  }
  return $values
}

function Test-ScalarisPort([int]$Port) {
  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
  try {
    $listener.Server.ExclusiveAddressUse = $true
    $listener.Start()
    return $true
  } catch { return $false } finally { $listener.Stop() }
}

function Initialize-ScalarisConfiguration($Paths, [int]$Port, [bool]$ExistingData) {
  if (Test-Path -LiteralPath $Paths.Env -PathType Leaf) {
    return Read-ScalarisConfiguration $Paths.Env
  }
  if ($ExistingData) {
    throw 'Ya existen datos de Scalaris y falta su archivo de claves. Recupere scalaris.env desde su copia privada; no se borro ni reinicio la base.'
  }
  Protect-ScalarisDirectory $Paths.Data
  $values = [ordered]@{
    SCALARIS_DATABASE_PASSWORD = New-ScalarisSecret
    SCALARIS_BOOTSTRAP_USERNAME = 'admin.scalaris'
    SCALARIS_BOOTSTRAP_PASSWORD = New-ScalarisSecret
    SCALARIS_MARKETING_USERNAME = 'marketing.scalaris'
    SCALARIS_MARKETING_PASSWORD = New-ScalarisSecret
    SCALARIS_MARKETING_DISPLAY_NAME = 'Marketing Scalaris'
    SCALARIS_FRONTEND_PORT = [string]$Port
  }
  $lines = @('# Configuracion privada de Scalaris CRM. Conservar junto con los respaldos.')
  foreach ($entry in $values.GetEnumerator()) { $lines += "$($entry.Key)=$($entry.Value)" }
  [System.IO.File]::WriteAllLines($Paths.Env, $lines, [System.Text.UTF8Encoding]::new($false))
  Protect-ScalarisFile $Paths.Env
  $access = @(
    'SCALARIS CRM - ACCESO INICIAL', '',
    "Direccion: http://127.0.0.1:$Port", '',
    'Cuenta para el trabajo diario de Marketing:',
    "Usuario: $($values.SCALARIS_MARKETING_USERNAME)",
    "Clave inicial: $($values.SCALARIS_MARKETING_PASSWORD)", '',
    'Cuenta del responsable para administrar usuarios:',
    "Usuario: $($values.SCALARIS_BOOTSTRAP_USERNAME)",
    "Clave inicial: $($values.SCALARIS_BOOTSTRAP_PASSWORD)", '',
    'Estas claves son privadas. No enviar este archivo por correo ni guardarlo en Git.',
    'Puede cambiar la clave desde el CRM. Este archivo conserva solamente las claves iniciales.',
    'Conservar scalaris.env para recuperar la instalacion junto con el respaldo de la base.'
  )
  [System.IO.File]::WriteAllLines($Paths.Access, $access, [System.Text.UTF8Encoding]::new($false))
  Protect-ScalarisFile $Paths.Access
  return Read-ScalarisConfiguration $Paths.Env
}

function Resolve-ScalarisDocker {
  $command = Get-Command docker -ErrorAction SilentlyContinue
  if ($command) { return $command.Source }
  $candidate = Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin\docker.exe'
  if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
  throw 'Para esta modalidad local se necesita Docker Desktop con contenedores Linux. Instale Docker Desktop desde https://www.docker.com/products/docker-desktop/ y vuelva a abrir el CRM. No se instalo ningun programa.'
}

function Invoke-ScalarisDocker([string]$Docker, [string[]]$Arguments, [switch]$Quiet) {
  # Arguments never contain passwords. Do not print Compose config or container environments.
  $previousPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    if ($Quiet) {
      $output = @(& $Docker @Arguments 2>&1)
    } else {
      & $Docker @Arguments
    }
    $code = $LASTEXITCODE
  } finally { $ErrorActionPreference = $previousPreference }
  if ($code -ne 0) {
    throw "Docker no pudo completar la operacion (codigo $code). Revise Docker Desktop y el estado del CRM; se conservaron los datos."
  }
  if ($Quiet) { return ($output | Out-String).Trim() }
}

function Assert-ScalarisLocalDocker([string]$Docker) {
  if ($env:DOCKER_HOST -and $env:DOCKER_HOST -notmatch '^(npipe|unix)://') {
    throw 'La conexion Docker apunta a un equipo remoto. Seleccione Docker Desktop local para proteger los datos de Scalaris.'
  }
  $context = Invoke-ScalarisDocker $Docker @('context', 'show') -Quiet
  $endpoint = Invoke-ScalarisDocker $Docker @('context', 'inspect', $context, '--format', '{{.Endpoints.docker.Host}}') -Quiet
  if ($endpoint -notmatch '^(npipe|unix)://') {
    throw 'El contexto Docker no es local. Seleccione Docker Desktop local antes de continuar.'
  }
  $previousPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    $null = & $Docker info --format '{{.OSType}}' 2>&1
    $ready = $LASTEXITCODE -eq 0
  } finally { $ErrorActionPreference = $previousPreference }
  if (-not $ready) {
    $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
    if (-not (Test-Path -LiteralPath $desktop -PathType Leaf)) {
      throw 'Docker no esta iniciado. Abra Docker Desktop y vuelva a intentar.'
    }
    Write-Host 'Iniciando Docker Desktop. Puede tardar unos minutos...'
    Start-Process -FilePath $desktop -WindowStyle Hidden | Out-Null
    $deadline = (Get-Date).AddSeconds(180)
    do {
      Start-Sleep -Seconds 3
      $ErrorActionPreference = 'Continue'
      try {
        $null = & $Docker info --format '{{.OSType}}' 2>&1
        $ready = $LASTEXITCODE -eq 0
      } finally { $ErrorActionPreference = $previousPreference }
    } while (-not $ready -and (Get-Date) -lt $deadline)
    if (-not $ready) { throw 'Docker Desktop todavia no esta listo. Espere a que termine de iniciar y vuelva a abrir el CRM.' }
  }
  $operatingSystem = Invoke-ScalarisDocker $Docker @('info', '--format', '{{.OSType}}') -Quiet
  if ($operatingSystem -ne 'linux') { throw 'Docker Desktop debe usar contenedores Linux para ejecutar este CRM.' }
  $null = Invoke-ScalarisDocker $Docker @('compose', 'version') -Quiet
}

function Get-ScalarisComposeArguments($Paths) {
  return @('compose', '--project-name', $Paths.Project, '--env-file', $Paths.Env, '-f', $Paths.Compose)
}

function Invoke-ScalarisBackup($Paths, [string]$Docker, [string[]]$ComposeArguments) {
  $container = Invoke-ScalarisDocker $Docker ($ComposeArguments + @('ps', '-q', 'postgres')) -Quiet
  if (-not $container -or $container -match '\s') { throw 'Abra el CRM antes de crear un respaldo.' }
  Protect-ScalarisDirectory $Paths.Backup
  $stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssfffZ')
  $baseName = "scalaris-crm-$stamp-$([guid]::NewGuid().ToString('N').Substring(0, 8))"
  $containerFile = "/tmp/$baseName.dump"
  $backupFile = Join-Path $Paths.Backup "$baseName.dump"
  $partialFile = "$backupFile.partial"
  try {
    Invoke-ScalarisDocker $Docker @('exec', $container, 'pg_dump', '-U', 'scalaris', '-d', 'scalaris_crm', '-Fc', '--compress=9', '--no-owner', '--no-privileges', '-f', $containerFile)
    $null = Invoke-ScalarisDocker $Docker @('exec', $container, 'pg_restore', '--list', $containerFile) -Quiet
    Invoke-ScalarisDocker $Docker @('cp', "${container}:${containerFile}", $partialFile)
    if (-not (Test-Path -LiteralPath $partialFile) -or (Get-Item -LiteralPath $partialFile).Length -eq 0) {
      throw 'El respaldo esta vacio; no se creo una copia valida.'
    }
    Move-Item -LiteralPath $partialFile -Destination $backupFile
    Protect-ScalarisFile $backupFile
    $checksum = (Get-FileHash -Algorithm SHA256 -LiteralPath $backupFile).Hash.ToLowerInvariant()
    $version = Invoke-ScalarisDocker $Docker @('exec', $container, 'pg_dump', '--version') -Quiet
    $metadata = [ordered]@{
      createdAtUtc = (Get-Date).ToUniversalTime().ToString('o')
      business = 'Scalaris'
      database = 'scalaris_crm'
      composeProject = $Paths.Project
      format = 'postgres-custom-compressed'
      postgresVersion = $version
      sha256 = $checksum
      sizeBytes = (Get-Item -LiteralPath $backupFile).Length
      archiveReadable = $true
      restoreTested = $false
    }
    [System.IO.File]::WriteAllText("$backupFile.json", ($metadata | ConvertTo-Json), [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText("$backupFile.sha256", "$checksum  $([System.IO.Path]::GetFileName($backupFile))`r`n", [System.Text.Encoding]::ASCII)
    Protect-ScalarisFile "$backupFile.json"
    Protect-ScalarisFile "$backupFile.sha256"
  } finally {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & $Docker exec $container rm -f $containerFile 2>$null | Out-Null } finally { $ErrorActionPreference = $previousPreference }
  }
  Write-Host "Respaldo guardado: $backupFile" -ForegroundColor Green
  Write-Host 'Conserve tambien scalaris.env en un lugar privado. La restauracion requiere al responsable tecnico.'
  return $backupFile
}

if ($FunctionsOnly) { return }

$savedEnvironment = @{}
$mutex = $null
$ownsMutex = $false
try {
  $paths = Get-ScalarisPaths $DataDirectory
  $mutex = [System.Threading.Mutex]::new($false, 'Local\ScalarisCRM-LocalOperation')
  $ownsMutex = $mutex.WaitOne(0)
  if (-not $ownsMutex) { throw 'Otra ventana esta operando el CRM. Espere a que termine y vuelva a intentar.' }
  if ($Action -eq 'Access') {
    if (-not (Test-Path -LiteralPath $paths.Access -PathType Leaf)) { throw 'El acceso inicial se crea al abrir el CRM por primera vez.' }
    Protect-ScalarisFile $paths.Access
    Start-Process -FilePath notepad.exe -ArgumentList ('"' + $paths.Access + '"') | Out-Null
    return
  }
  $docker = Resolve-ScalarisDocker
  Assert-ScalarisLocalDocker $docker
  $hadConfiguration = Test-Path -LiteralPath $paths.Env -PathType Leaf
  if (-not $hadConfiguration -and $Action -ne 'Start') { throw 'Esta instalacion todavia no se configuro. Abra AbrirCRMScalaris.cmd primero.' }
  if (-not $hadConfiguration) {
    $volumes = Invoke-ScalarisDocker $docker @('volume', 'ls', '--format', '{{.Name}}') -Quiet
    $existingVolumes = @($volumes -split '\r?\n') -contains "$($paths.Project)_scalaris_postgres"
    $port = 8787
    if (-not $existingVolumes) {
      $publications = Invoke-ScalarisDocker $docker @('ps', '--format', '{{.Ports}}') -Quiet
      while ($port -le 8797 -and ((-not (Test-ScalarisPort $port)) -or $publications -match ":$port->")) { $port++ }
      if ($port -gt 8797) { throw 'Los puertos locales 8787 a 8797 estan ocupados. Solicite al responsable configurar otro puerto.' }
    }
    $configuration = Initialize-ScalarisConfiguration $paths $port $existingVolumes
  } else {
    Protect-ScalarisDirectory $paths.Data
    Protect-ScalarisFile $paths.Env
    if (Test-Path -LiteralPath $paths.Access -PathType Leaf) { Protect-ScalarisFile $paths.Access }
    $configuration = Read-ScalarisConfiguration $paths.Env
  }
  # Compose gives process variables precedence over --env-file. Isolate this installation.
  foreach ($key in $configuration.Keys) {
    $savedEnvironment[$key] = [System.Environment]::GetEnvironmentVariable($key, 'Process')
    [System.Environment]::SetEnvironmentVariable($key, $null, 'Process')
  }
  $composeArguments = Get-ScalarisComposeArguments $paths
  $null = Invoke-ScalarisDocker $docker ($composeArguments + @('config', '--quiet')) -Quiet
  $url = "http://127.0.0.1:$($configuration.SCALARIS_FRONTEND_PORT)"
  switch ($Action) {
    'Start' {
      Write-Host 'Preparando Scalaris CRM. El primer inicio necesita Internet para descargar sus componentes.'
      $up = $composeArguments + @('up', '-d', '--wait', '--wait-timeout', '240')
      if (-not $NoBuild) { $up += '--build' }
      Invoke-ScalarisDocker $docker $up
      $health = Invoke-RestMethod -Uri "$url/actuator/health" -TimeoutSec 30
      if ($health.status -ne 'UP') { throw 'El CRM no termino de iniciar. Se conservaron sus datos.' }
      $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 30
      if ($response.StatusCode -ne 200) { throw 'La pantalla del CRM no esta disponible todavia.' }
      Write-Host "Scalaris CRM listo: $url" -ForegroundColor Green
      Write-Host 'Use la cuenta de Marketing indicada en su archivo privado de acceso inicial.'
      Write-Host "Acceso inicial: $($paths.Access)"
      if (-not $NoBrowser) {
        if (-not $hadConfiguration) { Start-Process -FilePath notepad.exe -ArgumentList ('"' + $paths.Access + '"') | Out-Null }
        Start-Process $url | Out-Null
      }
    }
    'Stop' {
      Invoke-ScalarisDocker $docker ($composeArguments + @('stop', '--timeout', '30'))
      Write-Host 'Scalaris CRM detenido. Los contactos, tareas y demas datos siguen guardados.' -ForegroundColor Green
    }
    'Backup' { $null = Invoke-ScalarisBackup $paths $docker $composeArguments }
    'Status' {
      Invoke-ScalarisDocker $docker ($composeArguments + @('ps'))
      Write-Host "Direccion local: $url"
      Write-Host "Respaldos: $($paths.Backup)"
    }
  }
} catch {
  Write-Host ''
  Write-Host $_.Exception.Message -ForegroundColor Red
  exit 1
} finally {
  foreach ($key in $savedEnvironment.Keys) { [System.Environment]::SetEnvironmentVariable($key, $savedEnvironment[$key], 'Process') }
  if ($ownsMutex) { $mutex.ReleaseMutex() }
  if ($mutex) { $mutex.Dispose() }
}
