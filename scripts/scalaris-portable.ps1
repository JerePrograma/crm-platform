param(
  [ValidateSet('Start','Stop','Backup','Restore','Status')][string]$Action = 'Start',
  [string]$PackageRoot = (Split-Path -Parent $PSScriptRoot),
  [string]$DataRoot = (Join-Path $env:LOCALAPPDATA 'ScalarisCRM\portable'),
  [string]$BackupPath,
  [switch]$NoBrowser
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$PackageRoot = [IO.Path]::GetFullPath($PackageRoot)
$DataRoot = [IO.Path]::GetFullPath($DataRoot)
foreach ($general in @([IO.Path]::GetPathRoot($DataRoot),$env:USERPROFILE,$env:LOCALAPPDATA,$env:APPDATA,$env:ProgramFiles,$env:ProgramData,$env:WINDIR)) {
  if ($general -and $DataRoot.TrimEnd('\').Equals($general.TrimEnd('\'),[StringComparison]::OrdinalIgnoreCase)) { throw 'Elegí una carpeta exclusiva para los datos del CRM.' }
}
if ($DataRoot.Equals($PackageRoot,[StringComparison]::OrdinalIgnoreCase) -or $DataRoot.StartsWith($PackageRoot.TrimEnd('\') + '\',[StringComparison]::OrdinalIgnoreCase) -or $PackageRoot.StartsWith($DataRoot.TrimEnd('\') + '\',[StringComparison]::OrdinalIgnoreCase)) { throw 'Los datos privados deben estar fuera de la carpeta de la aplicación.' }
$configPath = Join-Path $DataRoot 'configuracion.json'
$statePath = Join-Path $DataRoot 'procesos.json'
$logs = Join-Path $DataRoot 'logs'
$pgData = Join-Path $DataRoot 'postgres'
$pgBin = Join-Path $PackageRoot 'runtime\postgres\bin'
$javaExe = Join-Path $PackageRoot 'runtime\java\bin\java.exe'
$nodeExe = Join-Path $PackageRoot 'runtime\node\node.exe'
$appJar = Join-Path $PackageRoot 'app\crm.jar'
$serverScript = Join-Path $PackageRoot 'scripts\scalaris-web-server.mjs'
$mutex = New-Object Threading.Mutex($false, 'Local\ScalarisCRMPortable')
$locked = $false

function Protect-Directory([string]$Directory) {
  $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
  $system = New-Object Security.Principal.SecurityIdentifier('S-1-5-18')
  $acl = Get-Acl -LiteralPath $Directory
  $rules = @($acl.Access)
  $private = $acl.AreAccessRulesProtected -and $rules.Count -eq 2
  foreach ($rule in $rules) {
    $value = $rule.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value
    if ($value -notin @($sid.Value,$system.Value) -or $rule.AccessControlType -ne 'Allow' -or $rule.FileSystemRights -ne [Security.AccessControl.FileSystemRights]::FullControl) { $private = $false }
  }
  if ($private) { return }
  $acl.SetAccessRuleProtection($true, $false)
  foreach ($rule in @($acl.Access)) { $acl.RemoveAccessRuleSpecific($rule) }
  foreach ($identity in @($sid,$system)) { $acl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow'))) }
  Set-Acl -LiteralPath $Directory -AclObject $acl
}
function Write-Json([string]$File, $Value) {
  [IO.File]::WriteAllText($File, ($Value | ConvertTo-Json -Depth 6), (New-Object Text.UTF8Encoding($false)))
}
function New-Password {
  $bytes = New-Object byte[] 24
  $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
  try { $rng.GetBytes($bytes); return [Convert]::ToBase64String($bytes) } finally { $rng.Dispose() }
}
function Free-Port([int]$Start, [int[]]$Reserved = @()) {
  foreach ($port in $Start..($Start + 30)) {
    if ($Reserved -contains $port) { continue }
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, $port)
    try { $listener.Start(); return $port } catch {} finally { $listener.Stop() }
  }
  throw 'No hay un puerto local disponible. Cerrá otra instancia del CRM y volvé a abrirlo.'
}
function Require-FreePort([int]$Port) {
  $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, $Port)
  try { $listener.Start() } catch { throw "El puerto $Port está ocupado por otra aplicación. No se detuvo ese servicio. Cerrá la otra instancia o pedí ayuda para cambiar el puerto del CRM." } finally { $listener.Stop() }
}
function Run-Tool([string]$Exe, [string[]]$Arguments, [string]$Name) {
  # Esperar sólo pg_ctl: -Wait también espera a PostgreSQL y bloquea el arranque.
  $process = Start-Process -FilePath $Exe -ArgumentList $Arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs "$Name.out.log") -RedirectStandardError (Join-Path $logs "$Name.err.log")
  # Conservar el handle permite leer ExitCode después del cierre en PowerShell 5.1.
  $null = $process.Handle
  $process.WaitForExit()
  if ($process.ExitCode -ne 0) { throw "No se pudo completar $Name. Revisá la carpeta de registros: $logs" }
}
function Start-Helper([string]$Exe, [string[]]$Arguments, [string]$Name) {
  $process = Start-Process -FilePath $Exe -ArgumentList $Arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs "$Name.out.log") -RedirectStandardError (Join-Path $logs "$Name.err.log")
  return @{ id=$process.Id; ticks=$process.StartTime.ToUniversalTime().Ticks.ToString(); executable=$Exe; marker=$Arguments[-1].Trim('"') }
}
function Owned-Process($Record) {
  if ($null -eq $Record) { return $null }
  $process = Get-Process -Id $Record.id -ErrorAction SilentlyContinue
  if ($null -eq $process) { return $null }
  try {
    if ($process.StartTime.ToUniversalTime().Ticks.ToString() -ne $Record.ticks) { return $null }
    if ($process.Path -ne $Record.executable) { return $null }
    $native = Get-CimInstance Win32_Process -Filter "ProcessId = $($Record.id)"
    if (-not $native.CommandLine.Contains($Record.marker)) { return $null }
    return $process
  } catch { return $null }
}
function Wait-Healthy([string]$Url, [int]$StartedProcess = 0) {
  $deadline = [DateTime]::UtcNow.AddMinutes(3)
  do {
    if ($StartedProcess -ne 0 -and $null -eq (Get-Process -Id $StartedProcess -ErrorAction SilentlyContinue)) { throw "El CRM no pudo iniciar. Revisá los registros en $logs" }
    try {
      $health = Invoke-RestMethod -Uri $Url -TimeoutSec 3
      if ($health.status -eq 'UP') { return }
    } catch {}
    Start-Sleep -Seconds 2
  } while ([DateTime]::UtcNow -lt $deadline)
  throw "El CRM no terminó de iniciar. Revisá los registros en $logs"
}
function Stop-App {
  if (Test-Path -LiteralPath $statePath) {
    $saved = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    foreach ($record in @($saved.web, $saved.backend)) {
      $process = Owned-Process $record
      if ($null -ne $process) { Stop-Process -Id $process.Id; $null = $process.WaitForExit(10000) }
    }
    Remove-Item -LiteralPath $statePath -Force
  }
}
try {
  $locked = $mutex.WaitOne(1000)
  if (-not $locked) { throw 'Hay otra operación del CRM en curso. Esperá a que termine.' }
  if ($Action -ne 'Start' -and -not (Test-Path -LiteralPath $configPath)) { throw 'Primero abrí el CRM para crear la configuración local.' }
  foreach ($file in @($javaExe,$nodeExe,$appJar,$serverScript,(Join-Path $pgBin 'pg_ctl.exe'))) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw 'Extraé el ZIP completo del CRM antes de abrirlo. No lo ejecutes desde el interior del ZIP.' }
  }
  New-Item -ItemType Directory -Path $DataRoot,$logs -Force | Out-Null
  Protect-Directory $DataRoot
  $firstRun = -not (Test-Path -LiteralPath $configPath)
  if ($firstRun) {
    if (Test-Path -LiteralPath (Join-Path $pgData 'PG_VERSION')) { throw 'La base ya existe y falta su configuración privada. Recuperá configuracion.json; no se regeneraron contraseñas.' }
    $pgPort = Free-Port 18870
    $backendPort = Free-Port 18871 @($pgPort)
    $webPort = Free-Port 8787 @($pgPort,$backendPort)
    $config = [pscustomobject]@{ pgPort=$pgPort; backendPort=$backendPort; webPort=$webPort; database='scalaris_crm'; databasePassword=(New-Password); adminUsername='scalaris-admin'; adminPassword=(New-Password); marketingUsername='scalaris-marketing'; marketingPassword=(New-Password) }
    Write-Json $configPath $config
    $access = "SCALARIS CRM`r`n`r`nDirección: http://127.0.0.1:$webPort`r`n`r`nCuenta de Marketing`r`nUsuario: $($config.marketingUsername)`r`nContraseña inicial: $($config.marketingPassword)`r`n`r`nCuenta de administración`r`nUsuario: $($config.adminUsername)`r`nContraseña inicial: $($config.adminPassword)`r`n`r`nCambiá tu contraseña en Mi cuenta después de ingresar. Estas contraseñas iniciales no se actualizan cuando las cambiás dentro del CRM.`r`nConservá este archivo en privado."
    [IO.File]::WriteAllText((Join-Path $DataRoot 'Acceso-inicial.txt'), $access, (New-Object Text.UTF8Encoding($true)))
  } else { $config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json }
  $ports = @($config.pgPort,$config.backendPort,$config.webPort)
  if (@($ports | Select-Object -Unique).Count -ne 3) { throw 'La configuración tiene puertos repetidos.' }
  foreach ($value in $ports) { if ("$value" -notmatch '^\d+$' -or [int]$value -le 1024 -or [int]$value -gt 65535) { throw 'La configuración tiene un puerto inválido.' } }
  if ($config.database -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw 'El nombre de la base local es inválido.' }
  foreach ($password in @($config.databasePassword,$config.adminPassword,$config.marketingPassword)) { if ([string]::IsNullOrWhiteSpace($password) -or $password.Length -lt 12) { throw 'La configuración de acceso está incompleta.' } }
  $env:PGPASSWORD = $config.databasePassword
  $pgArguments = @('-h','127.0.0.1','-p',"$($config.pgPort)",'-U','scalaris')
  if ($Action -eq 'Status') {
    Write-Host "Datos y respaldos: $DataRoot"
    Write-Host "Dirección local: http://127.0.0.1:$($config.webPort)"
    try { $health = Invoke-RestMethod "http://127.0.0.1:$($config.webPort)/actuator/health" -TimeoutSec 3; Write-Host "Estado: $($health.status)" } catch { Write-Host 'Estado: detenido o iniciando' }
    return
  }
  if ($Action -eq 'Stop') {
    Stop-App
    if (Test-Path (Join-Path $pgData 'postmaster.pid')) { Run-Tool (Join-Path $pgBin 'pg_ctl.exe') @('-D', ('"' + $pgData + '"'), '-m','fast','-w','stop') 'detener-base' }
    Write-Host 'CRM detenido. Los contactos y respaldos se conservan.'
    return
  }
  if (-not (Test-Path -LiteralPath (Join-Path $pgData 'PG_VERSION'))) {
    $passwordFile = Join-Path $DataRoot 'inicializacion.tmp'
    [IO.File]::WriteAllText($passwordFile, $config.databasePassword)
    try { Run-Tool (Join-Path $pgBin 'initdb.exe') @('-D', ('"' + $pgData + '"'),'-U','scalaris','--encoding=UTF8','--locale=C','--auth=scram-sha-256', ('--pwfile="' + $passwordFile + '"')) 'inicializar-base' }
    finally { if (Test-Path -LiteralPath $passwordFile) { Remove-Item -LiteralPath $passwordFile -Force } }
  }
  $pgVersion = (Get-Content -LiteralPath (Join-Path $pgData 'PG_VERSION')).Trim()
  $runtimeVersion = (& (Join-Path $pgBin 'postgres.exe') --version) -join ''
  if (-not $runtimeVersion.Contains(" $pgVersion.")) { throw 'La versión de la base no coincide con este paquete. Usá el paquete anterior y generá un respaldo antes de actualizar.' }
  & (Join-Path $pgBin 'pg_ctl.exe') -D $pgData status *> $null
  if ($LASTEXITCODE -ne 0) {
    Run-Tool (Join-Path $pgBin 'pg_ctl.exe') @('-D', ('"' + $pgData + '"'),'-l', ('"' + (Join-Path $logs 'postgres.log') + '"'),'-o', ('"-h 127.0.0.1 -p ' + $config.pgPort + '"'),'-w','start') 'iniciar-base'
  }
  if ($Action -eq 'Backup') {
    $backups = Join-Path $DataRoot 'respaldos'
    New-Item -ItemType Directory -Path $backups -Force | Out-Null
    $backup = Join-Path $backups ("ScalarisCRM-" + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.dump')
    Run-Tool (Join-Path $pgBin 'pg_dump.exe') ($pgArguments + @('-d',$config.database,'-Fc','-f',('"' + $backup + '.partial"'))) 'respaldar'
    Run-Tool (Join-Path $pgBin 'pg_restore.exe') @('--list',('"' + $backup + '.partial"')) 'comprobar-respaldo'
    Move-Item -LiteralPath ($backup + '.partial') -Destination $backup
    (Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash | Set-Content -LiteralPath ($backup + '.sha256')
    Write-Host "Respaldo creado: $backup"
    return
  }
  if ($Action -eq 'Restore') {
    if ([string]::IsNullOrWhiteSpace($BackupPath) -or -not (Test-Path -LiteralPath $BackupPath -PathType Leaf)) { throw 'Indicá un respaldo .dump válido con -BackupPath.' }
    if (-not (Test-Path -LiteralPath ($BackupPath + '.sha256'))) { throw 'Falta el archivo .sha256 que acompaña al respaldo.' }
    if ((Get-FileHash -LiteralPath $BackupPath -Algorithm SHA256).Hash -ne (Get-Content -LiteralPath ($BackupPath + '.sha256')).Trim()) { throw 'El respaldo no coincide con su checksum. No se restauró.' }
    Run-Tool (Join-Path $pgBin 'pg_restore.exe') @('--list',('"' + $BackupPath + '"')) 'validar-restauracion'
    $restoredDatabase = 'scalaris_restore_' + (Get-Date -Format 'yyyyMMdd_HHmmss_fff')
    Run-Tool (Join-Path $pgBin 'createdb.exe') ($pgArguments + @($restoredDatabase)) 'crear-base-restaurada'
    Run-Tool (Join-Path $pgBin 'pg_restore.exe') ($pgArguments + @('-d',$restoredDatabase,'--no-owner','--exit-on-error',('"' + $BackupPath + '"'))) 'restaurar'
    Stop-App
    $config.database = $restoredDatabase
    Write-Json $configPath $config
    Write-Host 'Respaldo restaurado en una base nueva. La base anterior se conserva. Abrí el CRM nuevamente.'
    return
  }
  $dbExists = & (Join-Path $pgBin 'psql.exe') @pgArguments -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$($config.database)'"
  if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar la base local.' }
  if (($dbExists -join '').Trim() -ne '1') { Run-Tool (Join-Path $pgBin 'createdb.exe') ($pgArguments + @($config.database)) 'crear-base' }
  if (Test-Path -LiteralPath $statePath) {
    $saved = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    if ($null -ne (Owned-Process $saved.web) -and $null -ne (Owned-Process $saved.backend)) {
      Wait-Healthy "http://127.0.0.1:$($config.webPort)/actuator/health"
      if (-not $NoBrowser) { Start-Process "http://127.0.0.1:$($config.webPort)" }
      Write-Host 'El CRM ya estaba abierto.'
      return
    }
    Stop-App
  }
  Require-FreePort $config.backendPort
  Require-FreePort $config.webPort
  foreach ($variable in @('SPRING_APPLICATION_JSON','SPRING_CONFIG_LOCATION','SPRING_CONFIG_ADDITIONAL_LOCATION','JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS','NODE_OPTIONS')) { if (Test-Path "Env:$variable") { Remove-Item -LiteralPath "Env:$variable" } }
  $env:DATABASE_URL = "jdbc:postgresql://127.0.0.1:$($config.pgPort)/$($config.database)"
  $env:DATABASE_USER = 'scalaris'
  $env:DATABASE_PASSWORD = $config.databasePassword
  $env:CRM_BOOTSTRAP_USERNAME = $config.adminUsername
  $env:CRM_BOOTSTRAP_PASSWORD = $config.adminPassword
  $env:CRM_MARKETING_USERNAME = $config.marketingUsername
  $env:CRM_MARKETING_PASSWORD = $config.marketingPassword
  $env:CRM_MARKETING_DISPLAY_NAME = 'Marketing Scalaris'
  $env:CRM_BUSINESS_PROFILE = 'SCALARIS'
  $env:SPRING_PROFILES_ACTIVE = 'scalaris'
  $env:SERVER_ADDRESS = '127.0.0.1'
  $env:PORT = "$($config.backendPort)"
  $env:SESSION_COOKIE_SECURE = 'false'
  $env:SENDING_ENABLED = 'false'
  $env:SENDING_DRY_RUN = 'true'
  $env:SENDING_DAILY_LIMIT = '0'
  $env:SENDING_KILL_SWITCH = 'true'
  $env:MESSAGING_REAL_NETWORK_ALLOWED = 'false'
  $env:EMAIL_PROVIDER_MODE = 'NOOP'
  $env:WHATSAPP_PROVIDER_MODE = 'DEEPLINK_ONLY'
  $env:OUTBOX_WORKER_ENABLED = 'false'
  $env:FAKE_INBOUND_ENABLED = 'false'
  $env:SCALARIS_WEB_PORT = "$($config.webPort)"
  $env:SCALARIS_BACKEND_PORT = "$($config.backendPort)"
  $env:SCALARIS_WEB_ROOT = Join-Path $PackageRoot 'app\web'
  Write-Host 'Iniciando Scalaris CRM...'
  $backend = Start-Helper $javaExe @('-Xmx512m','-jar',('"' + $appJar + '"')) 'backend'
  Write-Json $statePath @{ backend=$backend; web=$null }
  Wait-Healthy "http://127.0.0.1:$($config.backendPort)/actuator/health" $backend.id
  $web = Start-Helper $nodeExe @(('"' + $serverScript + '"')) 'web'
  Write-Json $statePath @{ backend=$backend; web=$web }
  Wait-Healthy "http://127.0.0.1:$($config.webPort)/actuator/health"
  if (-not $NoBrowser) {
    Start-Process "http://127.0.0.1:$($config.webPort)"
    if ($firstRun) { Start-Process -FilePath notepad.exe -ArgumentList ('"' + (Join-Path $DataRoot 'Acceso-inicial.txt') + '"') }
  }
  Write-Host "CRM disponible: http://127.0.0.1:$($config.webPort)"
  Write-Host "Acceso inicial y datos privados: $DataRoot"
} finally {
  $env:PGPASSWORD = $null
  if ($locked) { $mutex.ReleaseMutex() }
  $mutex.Dispose()
}
