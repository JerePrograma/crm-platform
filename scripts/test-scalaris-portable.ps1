param([Parameter(Mandatory=$true)][string]$PackageRoot)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = Split-Path -Parent $PSScriptRoot
$launcher = Join-Path $PackageRoot 'scripts\scalaris-portable.ps1'
$data = Join-Path $env:LOCALAPPDATA ('ScalarisCRM\qa-portable-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$assertions = 0
function Check([bool]$Condition,[string]$Message) {
  if (-not $Condition) { throw "Regresión portable: $Message" }
  $script:assertions++
}
function Session($Config) {
  $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
  $base = "http://127.0.0.1:$($Config.webPort)"
  $csrf = Invoke-RestMethod "$base/api/v1/auth/csrf" -WebSession $session
  $headers = @{}; $headers[$csrf.headerName] = $csrf.token
  $user = Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -WebSession $session -Headers $headers -ContentType 'application/json; charset=utf-8' -Body (@{ username=$Config.marketingUsername; password=$Config.marketingPassword } | ConvertTo-Json -Compress)
  $csrf = Invoke-RestMethod "$base/api/v1/auth/csrf" -WebSession $session
  $headers = @{}; $headers[$csrf.headerName] = $csrf.token
  return @{ web=$session; user=$user; headers=$headers; base=$base }
}
try {
  & $launcher -PackageRoot $PackageRoot -DataRoot $data -NoBrowser
  $config = Get-Content (Join-Path $data 'configuracion.json') -Raw | ConvertFrom-Json
  $private = Get-Acl -LiteralPath $data
  Check $private.AreAccessRulesProtected 'la carpeta privada conserva herencia pública'
  Check (@($private.Access).Count -eq 2) 'los permisos privados difieren de usuario y SYSTEM'
  Check ($config.adminPassword -ne $config.marketingPassword -and $config.databasePassword -ne $config.adminPassword) 'se reutilizan contraseñas'
  $accessHash = (Get-FileHash -LiteralPath (Join-Path $data 'configuracion.json')).Hash
  $session = Session $config
  Check ($session.user.role -eq 'MARKETING') 'la cuenta operativa no tiene rol Marketing'
  Check ($session.user.permissions -notcontains 'USER_MANAGE' -and $session.user.permissions -notcontains 'MESSAGE_SEND') 'Marketing tiene administración técnica o envío real'
  $profile = Invoke-RestMethod "$($session.base)/api/v1/business-profile" -WebSession $session.web
  Check ($profile.code -eq 'SCALARIS' -and @($profile.serviceLines).Count -eq 3) 'perfil de negocio incorrecto'
  $tags = Invoke-RestMethod "$($session.base)/api/v1/tags" -WebSession $session.web
  $templates = Invoke-RestMethod "$($session.base)/api/v1/templates" -WebSession $session.web
  Check (@($tags).Count -eq 18 -and @($templates).Count -eq 7) 'no se crearon las etiquetas y plantillas'
  foreach ($port in @($config.pgPort,$config.backendPort,$config.webPort)) {
    $listeners = @(Get-NetTCPConnection -LocalPort $port -State Listen)
    Check ($listeners.Count -ge 1 -and @($listeners | Where-Object { $_.LocalAddress -ne '127.0.0.1' }).Count -eq 0) 'un componente escucha fuera de esta computadora'
  }
  $prospect = Invoke-RestMethod "$($session.base)/api/v1/prospects" -Method Post -WebSession $session.web -Headers $session.headers -ContentType 'application/json; charset=utf-8' -Body (@{ institutionName='Verificación ficticia del respaldo Scalaris'; locality='Localidad sintética'; source='MANUAL' } | ConvertTo-Json -Compress)
  Check (-not [string]::IsNullOrWhiteSpace($prospect.id)) 'no se guardó el prospecto sintético'
  & $launcher -PackageRoot $PackageRoot -DataRoot $data -NoBrowser
  Check ((Get-FileHash -LiteralPath (Join-Path $data 'configuracion.json')).Hash -eq $accessHash) 'reabrir cambia las claves'
  & $launcher -Action Backup -PackageRoot $PackageRoot -DataRoot $data -NoBrowser
  $backup = Get-ChildItem (Join-Path $data 'respaldos') -Filter '*.dump' | Select-Object -First 1
  Check ($null -ne $backup -and $backup.Length -gt 0) 'no se creó un respaldo'
  Check ((Get-FileHash -LiteralPath $backup.FullName).Hash -eq (Get-Content ($backup.FullName + '.sha256')).Trim()) 'checksum del respaldo incorrecto'
  $previousDatabase = $config.database
  & $launcher -Action Restore -BackupPath $backup.FullName -PackageRoot $PackageRoot -DataRoot $data -NoBrowser
  $restored = Get-Content (Join-Path $data 'configuracion.json') -Raw | ConvertFrom-Json
  Check ($restored.database -ne $previousDatabase -and $restored.database.StartsWith('scalaris_restore_')) 'la restauración sobrescribe la base original'
  & $launcher -PackageRoot $PackageRoot -DataRoot $data -NoBrowser
  $session = Session $restored
  $restoredProspect = Invoke-RestMethod "$($session.base)/api/v1/prospects/$($prospect.id)" -WebSession $session.web
  Check ($restoredProspect.id -eq $prospect.id -and $restoredProspect.displayName -eq $prospect.displayName) 'el prospecto no sobrevivió al respaldo/restauración'
  $env:PGPASSWORD = $restored.databasePassword
  $originalStillExists = & (Join-Path $PackageRoot 'runtime\postgres\bin\psql.exe') -h 127.0.0.1 -p $restored.pgPort -U scalaris -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$previousDatabase'"
  Check (($originalStillExists -join '').Trim() -eq '1') 'la base original desapareció'
  $sent = & (Join-Path $PackageRoot 'runtime\postgres\bin\psql.exe') -h 127.0.0.1 -p $restored.pgPort -U scalaris -d $restored.database -tAc "SELECT count(*) FROM message_record WHERE status IN ('SENT','DELIVERED','READ')"
  Check (($sent -join '').Trim() -eq '0') 'hay mensajes enviados reales'
  $summary = @{ status='EXECUTED_PASS'; assertions=$assertions; dataDirectory=$data; packageDirectory=$PackageRoot; backupRestore='EXECUTED_PASS'; originalDatabasePreserved=$true; realMessages=0; recordedAt=[DateTime]::UtcNow.ToString('o') }
  $evidence = Join-Path $repo 'validation-output\scalaris-portable-latest.json'
  New-Item -ItemType Directory -Path (Split-Path -Parent $evidence) -Force | Out-Null
  $summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $evidence -Encoding UTF8
  Write-Host "Portable Windows: $assertions comprobaciones aprobadas. Respaldo y restauración ejecutados."
} finally {
  if (Test-Path -LiteralPath (Join-Path $data 'configuracion.json')) { & $launcher -Action Stop -PackageRoot $PackageRoot -DataRoot $data -NoBrowser }
  if (Test-Path Env:PGPASSWORD) { Remove-Item Env:PGPASSWORD }
}
