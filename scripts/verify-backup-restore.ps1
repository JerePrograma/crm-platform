param(
  [string]$Database = $env:POSTGRES_DB,
  [string]$DatabaseUser = $env:DATABASE_USER
)

$ErrorActionPreference = 'Stop'
$container = (& docker compose ps -q postgres).Trim()
if (-not $container) { throw 'The project PostgreSQL container is not running.' }
if (-not $Database) { $Database = (& docker exec $container printenv POSTGRES_DB).Trim() }
if (-not $DatabaseUser) { $DatabaseUser = (& docker exec $container printenv POSTGRES_USER).Trim() }
$suffix = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss')
$source = "crm_restore_source_$suffix"
$target = "crm_restore_target_$suffix"
$firstDump = "/tmp/$source.dump"
$probeDump = "/tmp/$target.dump"
try {
  & docker exec $container pg_dump -U $DatabaseUser -d $Database -Fc --no-owner --no-privileges -f $firstDump
  if ($LASTEXITCODE -ne 0) { throw 'Initial synthetic-source dump failed.' }
  & docker exec $container createdb -U $DatabaseUser $source
  if ($LASTEXITCODE -ne 0) { throw 'Could not create isolated source database.' }
  & docker exec $container pg_restore -U $DatabaseUser -d $source --no-owner --no-privileges --exit-on-error $firstDump
  if ($LASTEXITCODE -ne 0) { throw 'Could not restore schema into isolated source.' }
  & docker exec $container psql -U $DatabaseUser -d $source -v ON_ERROR_STOP=1 -c "CREATE TABLE backup_restore_probe(id uuid PRIMARY KEY, value text NOT NULL); INSERT INTO backup_restore_probe VALUES ('00000000-0000-0000-0000-00000000b001','SYNTHETIC_BACKUP_RESTORE_PROBE');"
  if ($LASTEXITCODE -ne 0) { throw 'Could not create synthetic restore probe.' }
  & docker exec $container pg_dump -U $DatabaseUser -d $source -Fc --compress=9 --no-owner --no-privileges -f $probeDump
  if ($LASTEXITCODE -ne 0) { throw 'Synthetic backup failed.' }
  & docker exec $container createdb -U $DatabaseUser $target
  if ($LASTEXITCODE -ne 0) { throw 'Could not create isolated target database.' }
  & docker exec $container pg_restore -U $DatabaseUser -d $target --no-owner --no-privileges --exit-on-error $probeDump
  if ($LASTEXITCODE -ne 0) { throw 'Synthetic restore failed.' }
  $probe = (& docker exec $container psql -U $DatabaseUser -d $target -At -v ON_ERROR_STOP=1 -c "SELECT value FROM backup_restore_probe WHERE id='00000000-0000-0000-0000-00000000b001';").Trim()
  $version = (& docker exec $container psql -U $DatabaseUser -d $target -At -v ON_ERROR_STOP=1 -c 'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1;').Trim()
  if ($probe -ne 'SYNTHETIC_BACKUP_RESTORE_PROBE') { throw 'Synthetic probe integrity validation failed.' }
  if (-not $version) { throw 'Flyway history validation failed after restore.' }
  Write-Host "Backup/restore drill passed: isolated schema V$version and synthetic probe verified."
} finally {
  $cleanupPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  & docker exec $container dropdb -U $DatabaseUser --if-exists $target 2>$null | Out-Null
  & docker exec $container dropdb -U $DatabaseUser --if-exists $source 2>$null | Out-Null
  & docker exec $container rm -f $firstDump $probeDump 2>$null | Out-Null
  $ErrorActionPreference = $cleanupPreference
}
