$ErrorActionPreference = 'Stop'
$temporary = Join-Path ([System.IO.Path]::GetTempPath()) ("crm-database-test-$([guid]::NewGuid().ToString('N'))")
$global:crmDatabaseTestChecks = 0
$global:crmDatabaseTestCalls = New-Object System.Collections.Generic.List[object]
$global:crmDatabaseTestFailCopy = $false
$global:crmDatabaseTestMultipleContainers = $false
$global:crmDatabaseTestFailCreatedb = ''

function Assert([bool]$Condition, [string]$Message) {
  if (-not $Condition) { throw $Message }
  $global:crmDatabaseTestChecks++
}
function Expect-Failure([scriptblock]$Action, [string]$Message) {
  $failed = $false
  try { & $Action | Out-Null } catch {
    $failed = $true
    Assert ($_.Exception.Message -match $Message) ("Unexpected failure: " + $_.Exception.Message)
  }
  Assert $failed 'The unsafe database operation unexpectedly succeeded.'
}
function docker {
  param([Parameter(ValueFromRemainingArguments = $true)][object[]]$Arguments)
  $global:crmDatabaseTestCalls.Add(@($Arguments))
  $global:LASTEXITCODE = 0
  if ($Arguments[0] -eq 'compose') {
    if ($global:crmDatabaseTestMultipleContainers) { return @('synthetic-db', 'unexpected-second-db') }
    return 'synthetic-db'
  }
  if ($Arguments[0] -eq 'cp' -and $Arguments[1] -like 'synthetic-db:*') {
    if ($global:crmDatabaseTestFailCopy) { $global:LASTEXITCODE = 1; return }
    [System.IO.File]::WriteAllBytes($Arguments[2], [System.Text.Encoding]::ASCII.GetBytes('PGDMP-synthetic-not-a-real-backup'))
  }
  if ($Arguments -contains 'printenv') {
    if ($Arguments -contains 'POSTGRES_DB') { return 'synthetic_crm' }
    return 'synthetic_user'
  }
  if ($Arguments -contains '--version') { return 'pg_dump (PostgreSQL) synthetic' }
  if ($Arguments -contains 'createdb' -and
      (($global:crmDatabaseTestFailCreatedb -eq 'source' -and $Arguments[-1] -like 'crm_restore_source_*') -or
       ($global:crmDatabaseTestFailCreatedb -eq 'target' -and $Arguments[-1] -like 'crm_restore_target_*'))) {
    $global:LASTEXITCODE = 1
    return
  }
  if ($Arguments -contains 'psql') {
    if ($Arguments[-1] -like 'SELECT value*') { return 'SYNTHETIC_BACKUP_RESTORE_PROBE' }
    if ($Arguments[-1] -like 'SELECT version*') { return '14' }
  }
}

try {
  New-Item -ItemType Directory -Path $temporary | Out-Null
  $compose = Join-Path $temporary 'selected compose.yml'
  $envFile = Join-Path $temporary 'private synthetic.env'
  [System.IO.File]::WriteAllText($compose, 'services: {}')
  [System.IO.File]::WriteAllText($envFile, '# synthetic only')
  $options = @{ ComposeFile = $compose; ComposeProjectName = 'selected-synthetic-project'; EnvFile = $envFile }
  $backupDirectory = Join-Path $temporary 'backup files'
  & (Join-Path $PSScriptRoot 'backup-database.ps1') -BackupDirectory $backupDirectory @options
  $backup = @(Get-ChildItem -LiteralPath $backupDirectory -Filter '*.dump')
  Assert ($backup.Count -eq 1) 'A successful backup must publish one complete file.'
  Assert (@(Get-ChildItem -LiteralPath $backupDirectory -Filter '*.partial').Count -eq 0) 'No partial backup may remain.'
  $selection = @($global:crmDatabaseTestCalls | Where-Object { $_[0] -eq 'compose' })[0]
  Assert (($selection -join '|') -eq (@('compose', '--file', $compose, '--project-name', 'selected-synthetic-project', '--env-file', $envFile, 'ps', '-q', 'postgres') -join '|')) 'The requested file, project and environment must reach Compose without splitting paths.'
  $metadata = Get-Content -LiteralPath ($backup[0].FullName + '.json') -Raw | ConvertFrom-Json
  Assert ($metadata.restoreTested -eq $false) 'Archive readability must not be reported as a successful restore.'
  Assert (@($global:crmDatabaseTestCalls | Where-Object { $_ -contains 'pg_restore' -and $_ -contains '--list' }).Count -eq 1) 'Backups must check archive readability before publication.'
  $global:crmDatabaseTestFailCopy = $true
  Expect-Failure { & (Join-Path $PSScriptRoot 'backup-database.ps1') -BackupDirectory $backupDirectory @options } 'docker cp failed'
  $global:crmDatabaseTestFailCopy = $false
  Assert (@(Get-ChildItem -LiteralPath $backupDirectory -Filter '*.dump').Count -eq 1) 'A failed copy must not publish a completed backup.'
  $restoreScript = Join-Path $PSScriptRoot 'restore-database.ps1'
  Expect-Failure { & $restoreScript -BackupFile $backup[0].FullName -TargetDatabase 'synthetic_crm' -ConfirmDestructiveRestore @options } 'crm_restore_'
  $checksumFile = $backup[0].FullName + '.sha256'
  $originalChecksum = Get-Content -LiteralPath $checksumFile -Raw
  Remove-Item -LiteralPath $checksumFile
  $before = $global:crmDatabaseTestCalls.Count
  Expect-Failure { & $restoreScript -BackupFile $backup[0].FullName -TargetDatabase 'crm_restore_test' -ConfirmDestructiveRestore @options } 'checksum file is required'
  Assert ($global:crmDatabaseTestCalls.Count -eq $before) 'A missing checksum must fail before contacting Docker.'
  [System.IO.File]::WriteAllText($checksumFile, ('0' * 64))
  Expect-Failure { & $restoreScript -BackupFile $backup[0].FullName -TargetDatabase 'crm_restore_test' -ConfirmDestructiveRestore @options } 'checksum does not match'
  [System.IO.File]::WriteAllText($checksumFile, $originalChecksum)
  & $restoreScript -BackupFile $backup[0].FullName -TargetDatabase 'crm_restore_test' -ConfirmDestructiveRestore @options
  Assert (@($global:crmDatabaseTestCalls | Where-Object { $_ -contains 'dropdb' -and $_[-1] -ne 'crm_restore_test' }).Count -eq 0) 'Restore may only reset its explicitly isolated target.'
  $global:crmDatabaseTestMultipleContainers = $true
  Expect-Failure { & (Join-Path $PSScriptRoot 'backup-database.ps1') -BackupDirectory $backupDirectory @options } 'exactly one'
  $global:crmDatabaseTestMultipleContainers = $false
  $global:crmDatabaseTestCalls.Clear()
  & (Join-Path $PSScriptRoot 'verify-backup-restore.ps1')
  Assert (($global:crmDatabaseTestCalls[0] -join '|') -eq 'compose|ps|-q|postgres') 'Existing validators must keep the default Compose selection when no options are supplied.'
  Assert (@($global:crmDatabaseTestCalls | Where-Object { $_ -contains 'dropdb' -and $_[-1] -notlike 'crm_restore_*' }).Count -eq 0) 'Drill cleanup must only delete its isolated databases.'
  $firstSource = @($global:crmDatabaseTestCalls | Where-Object { $_ -contains 'createdb' -and $_[-1] -like 'crm_restore_source_*' })[0][-1]
  Assert ($firstSource -cmatch '^crm_restore_source_[0-9]+_[a-f0-9]{32}$') 'Drill names must include PID and 128 random bits within the PostgreSQL identifier limit.'
  Assert ($firstSource.Length -le 63) 'Drill names must not be truncated by PostgreSQL.'
  $global:crmDatabaseTestCalls.Clear()
  & (Join-Path $PSScriptRoot 'verify-backup-restore.ps1')
  $secondSource = @($global:crmDatabaseTestCalls | Where-Object { $_ -contains 'createdb' -and $_[-1] -like 'crm_restore_source_*' })[0][-1]
  Assert ($secondSource -ne $firstSource) 'Consecutive drills must never reuse a database name.'
  $global:crmDatabaseTestCalls.Clear()
  $global:crmDatabaseTestFailCreatedb = 'source'
  Expect-Failure { & (Join-Path $PSScriptRoot 'verify-backup-restore.ps1') } 'create isolated source'
  Assert (@($global:crmDatabaseTestCalls | Where-Object { $_ -contains 'dropdb' }).Count -eq 0) 'A failed createdb must never grant ownership or delete an existing source/target database.'
  $global:crmDatabaseTestCalls.Clear()
  $global:crmDatabaseTestFailCreatedb = 'target'
  Expect-Failure { & (Join-Path $PSScriptRoot 'verify-backup-restore.ps1') } 'create isolated target'
  $ownedSource = @($global:crmDatabaseTestCalls | Where-Object { $_ -contains 'createdb' -and $_[-1] -like 'crm_restore_source_*' })[0][-1]
  $drops = @($global:crmDatabaseTestCalls | Where-Object { $_ -contains 'dropdb' })
  Assert ($drops.Count -eq 1 -and $drops[0][-1] -eq $ownedSource) 'A failed target creation may only clean the source actually created by this drill.'
  $global:crmDatabaseTestFailCreatedb = ''
  Write-Host "Database operation regressions passed: $global:crmDatabaseTestChecks assertions; Docker mocked, no operational database used."
} finally {
  $resolved = [System.IO.Path]::GetFullPath($temporary)
  $base = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
  if ($resolved.StartsWith($base, [System.StringComparison]::OrdinalIgnoreCase) -and
      [System.IO.Path]::GetFileName($resolved) -like 'crm-database-test-*' -and
      (Test-Path -LiteralPath $resolved)) {
    Remove-Item -LiteralPath $resolved -Recurse -Force
  }
}
