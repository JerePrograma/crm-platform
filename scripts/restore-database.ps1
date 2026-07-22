param(
  [Parameter(Mandatory = $true)][string]$BackupFile,
  [Parameter(Mandatory = $true)][string]$TargetDatabase,
  [string]$DatabaseUser = $env:DATABASE_USER,
  [switch]$ConfirmDestructiveRestore
)

$ErrorActionPreference = 'Stop'
if (-not $ConfirmDestructiveRestore) { throw 'Pass -ConfirmDestructiveRestore after reviewing the isolated target.' }
if ($TargetDatabase -notmatch '^crm_restore_[a-z0-9_]+$') { throw 'TargetDatabase must use the crm_restore_ prefix.' }
$resolvedBackup = (Resolve-Path -LiteralPath $BackupFile).Path
$expectedFile = "$resolvedBackup.sha256"
if (Test-Path -LiteralPath $expectedFile) {
  $expected = ((Get-Content -LiteralPath $expectedFile -Raw).Trim() -split '\s+')[0]
  $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedBackup).Hash.ToLowerInvariant()
  if ($actual -ne $expected) { throw 'Backup checksum does not match.' }
}
$container = (& docker compose ps -q postgres).Trim()
if (-not $container) { throw 'The project PostgreSQL container is not running.' }
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
