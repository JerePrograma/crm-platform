$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$launcher = Join-Path $PSScriptRoot 'scalaris-local.ps1'
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("scalaris-launcher-test-$([guid]::NewGuid().ToString('N'))")
$checks = 0

function Assert([bool]$Condition, [string]$Message) {
  if (-not $Condition) { throw $Message }
  $script:checks++
}

function Expect-Failure([scriptblock]$Operation, [string]$ExpectedMessage) {
  $didFail = $false
  try { & $Operation | Out-Null } catch {
    $didFail = $true
    Assert ($_.Exception.Message -match $ExpectedMessage) ("Failure did not explain the expected condition: " + $_.Exception.Message + "`n" + $_.ScriptStackTrace)
  }
  Assert $didFail 'Unsafe configuration unexpectedly passed.'
}

function Assert-PrivateAcl([string]$Path) {
  $acl = Get-Acl -LiteralPath $Path
  $allowedSids = @(
    [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value,
    'S-1-5-18'
  )
  Assert $acl.AreAccessRulesProtected 'Private files must not inherit broad access.'
  foreach ($rule in $acl.Access) {
    $sid = $rule.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value
    Assert ($allowedSids -contains $sid) 'Private files grant access beyond the current Windows user and SYSTEM.'
  }
}

try {
  $tokens = $null
  $parseErrors = $null
  [System.Management.Automation.Language.Parser]::ParseFile($launcher, [ref]$tokens, [ref]$parseErrors) | Out-Null
  Assert (@($parseErrors).Count -eq 0) 'Launcher PowerShell syntax is invalid.'
  . $launcher -FunctionsOnly

  $paths = Get-ScalarisPaths (Join-Path $tempRoot 'private-data')
  Expect-Failure { Get-ScalarisPaths (Join-Path $paths.Repo 'private-data') } 'fuera'
  Expect-Failure { Get-ScalarisPaths $paths.Repo } 'fuera'
  Expect-Failure { Get-ScalarisPaths (Split-Path -Parent $paths.Repo) } 'contenedora'
  Expect-Failure { Get-ScalarisPaths $env:LOCALAPPDATA } 'exclusiva'
  Assert ((Get-ScalarisPaths ($paths.Repo + '-sibling')).Data -eq ($paths.Repo + '-sibling')) 'A sibling outside the repository should remain usable.'

  $first = Initialize-ScalarisConfiguration $paths 8787 $false
  Assert ($first.SCALARIS_MARKETING_USERNAME -ne $first.SCALARIS_BOOTSTRAP_USERNAME) 'Marketing and administrator accounts must differ.'
  Assert ($first.SCALARIS_DATABASE_PASSWORD -match '^[a-f0-9]{48}$') 'Database secret must have 192 bits of random material.'
  Assert ($first.SCALARIS_MARKETING_PASSWORD -ne $first.SCALARIS_BOOTSTRAP_PASSWORD) 'Generated accounts must have separate passwords.'
  Assert-PrivateAcl $paths.Env
  Assert-PrivateAcl $paths.Access
  Protect-ScalarisDirectory $paths.Data
  Protect-ScalarisFile $paths.Env
  Assert-PrivateAcl $paths.Env
  $before = [System.IO.File]::ReadAllText($paths.Env)
  $initialAccess = [System.IO.File]::ReadAllText($paths.Access)
  Assert ($initialAccess.Contains($first.SCALARIS_MARKETING_PASSWORD)) 'The local initial-access file must allow Marketing to sign in.'
  $second = Initialize-ScalarisConfiguration $paths 8790 $true
  Assert ([System.IO.File]::ReadAllText($paths.Env) -eq $before) 'A repeated launch must preserve the existing credentials byte for byte.'
  Assert ($second.SCALARIS_FRONTEND_PORT -eq '8787') 'A repeated launch must preserve the chosen port.'

  $orphanPaths = Get-ScalarisPaths (Join-Path $tempRoot 'lost-keys')
  Expect-Failure { Initialize-ScalarisConfiguration $orphanPaths 8787 $true } 'Ya existen datos'
  Assert (-not (Test-Path -LiteralPath $orphanPaths.Env)) 'An existing database without configuration must never trigger replacement credentials.'

  $invalidPath = Join-Path $paths.Data 'invalid.env'
  [System.IO.File]::WriteAllText($invalidPath, $before + "`nSCALARIS_FRONTEND_PORT=8788")
  Expect-Failure { Read-ScalarisConfiguration $invalidPath } 'repetidos'
  [System.IO.File]::WriteAllText($invalidPath, $before.Replace('SCALARIS_FRONTEND_PORT=8787', 'SCALARIS_FRONTEND_PORT=80'))
  Expect-Failure { Read-ScalarisConfiguration $invalidPath } 'puerto local'
  [System.IO.File]::WriteAllText($invalidPath, $before.Replace('SCALARIS_FRONTEND_PORT=8787', 'SCALARIS_FRONTEND_PORT=8787$HOME'))
  Expect-Failure { Read-ScalarisConfiguration $invalidPath } 'formato invalido'
  [System.IO.File]::WriteAllText($invalidPath, ($before -replace '(?m)^SCALARIS_MARKETING_PASSWORD=.*\r?\n?', ''))
  Expect-Failure { Read-ScalarisConfiguration $invalidPath } 'Falta SCALARIS_MARKETING_PASSWORD'
  [System.IO.File]::WriteAllText($invalidPath, $before + "`nSENDING_ENABLED=true")
  Expect-Failure { Read-ScalarisConfiguration $invalidPath } 'formato invalido'

  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
  try {
    $listener.Server.ExclusiveAddressUse = $true
    $listener.Start()
    $occupiedPort = ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    Assert (-not (Test-ScalarisPort $occupiedPort)) 'The launcher must reject an already occupied local port.'
  } finally { $listener.Stop() }
  Assert (Test-ScalarisPort $occupiedPort) 'The launcher should accept the port after its listener stops.'

  $arguments = Get-ScalarisComposeArguments $paths
  Assert ($arguments -contains 'scalaris-crm-local') 'Every Compose action must target the isolated Scalaris project.'
  Assert ($arguments -contains $paths.Env) 'Every Compose action must use the external private configuration.'
  Assert ($arguments -contains $paths.Compose) 'Every Compose action must use the Scalaris Compose file.'

  # Exercise backup orchestration without a Docker daemon; this is not a restore test.
  $script:dockerCalls = New-Object System.Collections.Generic.List[object]
  $script:failBackupCopy = $false
  function Invoke-ScalarisDocker([string]$Docker, [string[]]$Arguments, [switch]$Quiet) {
    $script:dockerCalls.Add(@($Arguments))
    if ($Arguments -contains 'postgres' -and $Arguments -contains 'ps') { return 'synthetic-postgres-id' }
    if ($Arguments[0] -eq 'cp') {
      if ($script:failBackupCopy) { throw 'Synthetic copy failure.' }
      [System.IO.File]::WriteAllBytes($Arguments[2], [System.Text.Encoding]::ASCII.GetBytes('PGDMP-synthetic-test-only'))
    }
    if ($Arguments -contains '--version') { return 'pg_dump (PostgreSQL) synthetic' }
  }
  $fakeDockerPath = Join-Path $tempRoot 'docker-cleanup.ps1'
  [System.IO.File]::WriteAllText($fakeDockerPath, 'param([Parameter(ValueFromRemainingArguments=$true)]$Arguments)')
  $backup = Invoke-ScalarisBackup $paths $fakeDockerPath $arguments
  Assert (Test-Path -LiteralPath $backup -PathType Leaf) 'Successful copy must create a backup file.'
  Assert (-not (Test-Path -LiteralPath "$backup.partial")) 'A successful backup must not leave a partial file.'
  Assert-PrivateAcl $backup
  $metadata = [System.IO.File]::ReadAllText("$backup.json") | ConvertFrom-Json
  Assert ($metadata.restoreTested -eq $false) 'Archive readability must never be described as proof of restoration.'
  Assert ($metadata.sha256 -eq (Get-FileHash -Algorithm SHA256 -LiteralPath $backup).Hash.ToLowerInvariant()) 'Backup metadata must match the saved bytes.'
  Assert (@($script:dockerCalls | Where-Object { $_ -contains 'pg_restore' -and $_ -contains '--list' }).Count -eq 1) 'Backup must inspect the dump archive before accepting it.'
  Assert (@($script:dockerCalls | Where-Object { $_ -contains 'pg_dump' -and $_ -contains 'scalaris_crm' }).Count -eq 1) 'Backup must dump only the Scalaris database.'
  $script:failBackupCopy = $true
  Expect-Failure { Invoke-ScalarisBackup $paths $fakeDockerPath $arguments } 'Synthetic copy failure'
  Assert (@(Get-ChildItem -LiteralPath $paths.Backup -Filter '*.dump').Count -eq 1) 'Failed copies must never appear as completed backups.'

  Write-Host "Scalaris local launcher focused tests passed: $checks assertions. Docker services and real restore were not executed."
} finally {
  $resolvedTemp = [System.IO.Path]::GetFullPath($tempRoot)
  $expectedBase = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
  if ($resolvedTemp.StartsWith($expectedBase, [System.StringComparison]::OrdinalIgnoreCase) -and
      [System.IO.Path]::GetFileName($resolvedTemp) -like 'scalaris-launcher-test-*' -and
      (Test-Path -LiteralPath $resolvedTemp)) {
    Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
  }
}
