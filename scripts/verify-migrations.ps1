param([string]$BackendImage)

$ErrorActionPreference = 'Stop'
$suffix = "$PID-$((Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss'))"
$network = "crm-migration-$suffix"
$postgres = "crm-migration-postgres-$suffix"
$upgrade11 = "crm-migration-v11-$suffix"
$upgradeLatest = "crm-migration-latest-$suffix"
$emptyLatest = "crm-migration-empty-$suffix"
$password = 'synthetic-migration-password'
if ([string]::IsNullOrWhiteSpace($BackendImage)) {
  $BackendImage = (& docker compose images -q backend).Trim()
}
if (-not $BackendImage) { throw 'Build the backend image before migration verification.' }

function Wait-Container([string]$Name, [int]$Attempts = 90) {
  for ($attempt = 0; $attempt -lt $Attempts; $attempt++) {
    $state = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $Name 2>$null).Trim()
    if ($state -eq 'healthy') { return }
    if ($state -in @('unhealthy','exited','dead')) { & docker logs $Name; throw "$Name failed with state $state" }
    Start-Sleep -Seconds 2
  }
  throw "$Name did not become healthy."
}

function Start-Backend([string]$Name, [string]$Database, [string]$Target) {
  $arguments = @('run','-d','--name',$Name,'--network',$network,
    '--env',"DATABASE_URL=jdbc:postgresql://${postgres}:5432/$Database",
    '--env','DATABASE_USER=gestudio','--env',"DATABASE_PASSWORD=$password",
    '--env','CRM_BOOTSTRAP_USERNAME=migration-admin','--env','CRM_BOOTSTRAP_PASSWORD=synthetic-migration-admin-password',
    '--env','SENDING_ENABLED=false','--env','SENDING_DRY_RUN=true','--env','SENDING_DAILY_LIMIT=0','--env','SENDING_KILL_SWITCH=true',
    '--env','MESSAGING_REAL_NETWORK_ALLOWED=false','--env','OUTBOX_WORKER_ENABLED=false','--env','FAKE_INBOUND_ENABLED=false')
  if ($Target) { $arguments += @('--env',"SPRING_FLYWAY_TARGET=$Target") }
  $arguments += $BackendImage
  & docker @arguments | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "Unable to start migration backend $Name" }
  Wait-Container $Name
}

try {
  & docker network create $network | Out-Null
  & docker run -d --name $postgres --network $network --env POSTGRES_DB=upgrade --env POSTGRES_USER=gestudio --env "POSTGRES_PASSWORD=$password" --health-cmd='pg_isready -U gestudio -d upgrade' --health-interval=2s --health-timeout=2s --health-retries=30 postgres:17-alpine | Out-Null
  if ($LASTEXITCODE -ne 0) { throw 'Unable to start isolated PostgreSQL.' }
  Wait-Container $postgres
  & docker exec $postgres createdb -h 127.0.0.1 -U gestudio empty
  if ($LASTEXITCODE -ne 0) { throw 'Unable to create empty migration database.' }

  Start-Backend $upgrade11 'upgrade' '11'
  $v11 = (& docker exec $postgres psql -h 127.0.0.1 -U gestudio -d upgrade -At -c 'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1;').Trim()
  if ($v11 -ne '11') { throw "Expected V11 baseline, found V$v11" }
  & docker rm -f $upgrade11 | Out-Null

  Start-Backend $upgradeLatest 'upgrade' ''
  $upgraded = (& docker exec $postgres psql -h 127.0.0.1 -U gestudio -d upgrade -At -c 'SELECT string_agg(version, '','' ORDER BY installed_rank) FROM flyway_schema_history WHERE success AND installed_rank > 11;').Trim()
  if ($upgraded -ne '12,13') { throw "Expected V11 to V13 upgrade, found $upgraded" }

  Start-Backend $emptyLatest 'empty' ''
  $emptyVersion = (& docker exec $postgres psql -h 127.0.0.1 -U gestudio -d empty -At -c 'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1;').Trim()
  if ($emptyVersion -ne '13') { throw "Expected empty migration to V13, found V$emptyVersion" }
  Write-Host 'Migration verification passed: empty -> V13, V11 -> V12 -> V13, Hibernate validate on both latest schemas.'
} finally {
  $cleanupPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  foreach ($name in @($emptyLatest,$upgradeLatest,$upgrade11,$postgres)) {
    foreach ($id in @(& docker ps -aq --filter "name=$name")) { & docker rm -f $id | Out-Null }
  }
  foreach ($id in @(& docker network ls -q --filter "name=$network")) { & docker network rm $id | Out-Null }
  $ErrorActionPreference = $cleanupPreference
}
