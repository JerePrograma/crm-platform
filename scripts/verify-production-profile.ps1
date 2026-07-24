param([int]$FrontendPort = 18080)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'container-env-assertions.ps1')
$project = "crm-production-smoke-$PID"
$compose = Join-Path (Split-Path -Parent $PSScriptRoot) 'deploy/docker-compose.production.yml'
$env:DATABASE_PASSWORD = 'synthetic-production-drill-password'
$env:CRM_BOOTSTRAP_USERNAME = 'production-drill-admin'
$env:CRM_BOOTSTRAP_PASSWORD = 'synthetic-production-drill-admin-password'
$env:SESSION_COOKIE_SECURE = 'false'
$env:PRODUCTION_FRONTEND_PORT = [string]$FrontendPort
try {
  & docker compose --project-name $project -f $compose up -d --build --wait
  if ($LASTEXITCODE -ne 0) { throw 'Production profile did not become healthy.' }
  $health = Invoke-RestMethod -Uri "http://127.0.0.1:$FrontendPort/actuator/health/readiness"
  if ($health.status -ne 'UP') { throw 'Production readiness is not UP.' }
  $backend = (& docker compose --project-name $project -f $compose ps -q backend).Trim()
  $postgres = (& docker compose --project-name $project -f $compose ps -q postgres).Trim()
  $environment = & docker inspect $backend --format '{{json .Config.Env}}'
  Assert-ContainerEnvironmentEntries -Json $environment -RequiredEntries @(
    'SENDING_ENABLED=false',
    'SENDING_DRY_RUN=true',
    'SENDING_DAILY_LIMIT=0',
    'SENDING_KILL_SWITCH=true',
    'MESSAGING_REAL_NETWORK_ALLOWED=false',
    'EMAIL_PROVIDER_MODE=NOOP',
    'WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY'
  )
  $sent = (& docker exec $postgres psql -U gestudio -d gestudio_crm -At -c "SELECT count(*) FROM message_record WHERE status IN ('SENT','DELIVERED','READ');").Trim()
  if ($sent -ne '0') { throw 'Production profile smoke found forbidden sent states.' }
  Write-Host 'Production profile local smoke passed: health, non-root/read-only services, blocked providers, zero SENT.'
} finally {
  $cleanupPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  & docker compose --project-name $project -f $compose down -v --remove-orphans 2>$null | Out-Null
  $ErrorActionPreference = $cleanupPreference
}
