param(
  [Parameter(Mandatory = $true)][string]$BackupFile,
  [Parameter(Mandatory = $true)][string]$TargetDatabase,
  [string]$DatabaseUser = $env:DATABASE_USER,
  [switch]$ConfirmDestructiveRestore,
  [string]$ComposeFile,
  [string]$ComposeProjectName,
  [string]$EnvFile
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'database-compose.ps1')
$composeArguments = @(Get-DatabaseComposeArguments $ComposeFile $ComposeProjectName $EnvFile)
if (-not $ConfirmDestructiveRestore) { throw 'Pass -ConfirmDestructiveRestore after reviewing the isolated target.' }
if ($TargetDatabase -notmatch '^crm_restore_[a-z0-9_]+$') { throw 'TargetDatabase must use the crm_restore_ prefix.' }
$resolvedBackup = (Resolve-Path -LiteralPath $BackupFile).Path
$expectedFile = "$resolvedBackup.sha256"
if (-not (Test-Path -LiteralPath $expectedFile -PathType Leaf)) { throw 'Backup checksum file is required.' }
$expected = ((Get-Content -LiteralPath $expectedFile -Raw).Trim() -split '\s+')[0]
$actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedBackup).Hash.ToLowerInvariant()
if ($expected -notmatch '^[a-fA-F0-9]{64}$' -or $actual -ne $expected) { throw 'Backup checksum does not match.' }
$container = Get-DatabaseContainer $composeArguments
if (-not $DatabaseUser) { $DatabaseUser = (& docker exec $container printenv POSTGRES_USER).Trim() }
$primary = if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { (& docker exec $container printenv POSTGRES_DB).Trim() }
if ($TargetDatabase -eq $primary) { throw 'Restoring over the primary project database is prohibited.' }
$containerFile = "/tmp/$([System.IO.Path]::GetFileName($resolvedBackup))"
try {
  & docker cp $resolvedBackup "${container}:${containerFile}"
  if ($LASTEXITCODE -ne 0) { throw 'docker cp failed.' }
  & docker exec $container dropdb -U $DatabaseUser --if-exists $TargetDatabase
  if ($LASTEXITCODE -ne 0) { throw 'Unable to reset isolated restore database.' }
  & docker exec $container createdb -U $DatabaseUser $TargetDatabase
  if ($LASTEXITCODE -ne 0) { throw 'Unable to create isolated restore database.' }
  & docker exec $container pg_restore -U $DatabaseUser -d $TargetDatabase --no-owner --no-privileges --exit-on-error $containerFile
  if ($LASTEXITCODE -ne 0) { throw 'pg_restore failed.' }
} finally {
  & docker exec $container rm -f $containerFile 2>$null | Out-Null
}
Write-Host "Restore completed into isolated database: $TargetDatabase"
