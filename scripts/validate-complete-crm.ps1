param(
  [ValidateRange(1, 65535)][int]$PostgresPort = 25432,
  [ValidateRange(1, 65535)][int]$BackendPort = 8080,
  [ValidateRange(1, 65535)][int]$FrontendPort = 5173,
  [ValidateRange(1, 65535)][int]$ProductionFrontendPort = 18080,
  [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'container-env-assertions.ps1')
$outputDirectory = Join-Path $repoRoot 'validation-output'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmss')
$previousComposeProject = $env:COMPOSE_PROJECT_NAME
$validationComposeProject = "crm-complete-$PID-$stamp".ToLowerInvariant()
$env:COMPOSE_PROJECT_NAME = $validationComposeProject
$transcriptPath = Join-Path $outputDirectory "complete-crm-$stamp.log"
$summaryPath = Join-Path $outputDirectory "complete-crm-$stamp.json"
$started = (Get-Date).ToUniversalTime()
$summary = [ordered]@{
  schemaVersion = 1
  validation = 'COMPLETE CRM local functional validation'
  status = 'RUNNING'
  platform = "Windows PowerShell $($PSVersionTable.PSVersion)"
  commit = $null
  branch = $null
  composeProject = $validationComposeProject
  startedAtUtc = $started.ToString('o')
  finishedAtUtc = $null
  durationSeconds = $null
  ports = [ordered]@{ postgres = $PostgresPort; backend = $BackendPort; frontend = $FrontendPort; productionFrontend = $ProductionFrontendPort }
  sendingBoundary = [ordered]@{ enabled = $false; dryRun = $true; dailyLimit = 0; killSwitch = $true; realNetwork = $false }
  phases = [ordered]@{}
  generatedEvidence = [ordered]@{ transcript = $transcriptPath; summary = $summaryPath; e2e = 'validation-output/complete-crm-e2e-latest.json' }
  error = $null
}

$phaseNames = @('trackedTreeClean','tooling','repositorySafety','scriptSyntax','secretScan','backendFormatUnitIntegrationArchitectureSecurity','frontendInstall','frontendTypecheck','frontendUnit','frontendBuild','composeNoCacheHealthSmoke','dependencyScan','migrationFromEmpty','migrationFromV11','outboxWorkersInboundWebhook','frontendE2E','gmailLiveFakeE2E','effectiveSendingBlockade','zeroSent','backupRestore','productionProfileSmoke','finalTreeClean')
foreach ($name in $phaseNames) { $summary.phases[$name] = [ordered]@{ status = 'NOT_RUN'; durationSeconds = $null } }

function Run-Phase([string]$Name, [scriptblock]$Action) {
  Write-Host "`n=== $Name ==="
  $phaseStart = Get-Date
  $summary.phases[$Name].status = 'RUNNING'
  try {
    & $Action
    $summary.phases[$Name].status = 'FUNCTIONAL_PASS'
  } catch {
    $summary.phases[$Name].status = 'EXECUTED_FAIL'
    throw
  } finally {
    $summary.phases[$Name].durationSeconds = [math]::Round(((Get-Date) - $phaseStart).TotalSeconds, 3)
  }
}

function Invoke-Checked([string]$Command, [string[]]$Arguments, [string]$WorkingDirectory = $repoRoot) {
  Write-Host "> $Command $($Arguments -join ' ')"
  Push-Location $WorkingDirectory
  try { & $Command @Arguments; if ($LASTEXITCODE -ne 0) { throw "Command failed with exit code ${LASTEXITCODE}: $Command" } }
  finally { Pop-Location }
}

Push-Location $repoRoot
Start-Transcript -Path $transcriptPath -Force | Out-Null
try {
  $summary.commit = (& git rev-parse HEAD).Trim()
  $summary.branch = (& git branch --show-current).Trim()
  Run-Phase 'trackedTreeClean' {
    if ($summary.branch -ne 'main') { throw "Expected main, found $($summary.branch)" }
    $changes = @(& git status --porcelain)
    if ($changes.Count -ne 0) { throw "Validation requires a clean tracked tree:`n$($changes -join "`n")" }
  }
  Run-Phase 'tooling' {
    foreach ($command in @('git','docker','node','npm','mvn')) { if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "Required command missing: $command" } }
    Invoke-Checked 'docker' @('info')
    Invoke-Checked 'docker' @('compose','version')
    & (Join-Path $PSScriptRoot 'check-host-ports.ps1') `
      -PostgresPort $PostgresPort `
      -BackendPort $BackendPort `
      -FrontendPort $FrontendPort `
      -ProductionFrontendPort $ProductionFrontendPort
  }
  Run-Phase 'repositorySafety' { & (Join-Path $PSScriptRoot 'check-repository-safety.ps1') }
  Run-Phase 'scriptSyntax' {
    & (Join-Path $PSScriptRoot 'check-powershell-syntax.ps1')
    & (Join-Path $PSScriptRoot 'test-container-env-assertions.ps1')
    & (Join-Path $PSScriptRoot 'test-check-host-ports.ps1')
    Invoke-Checked 'node' @('scripts/test-container-env-assertions.js')
    Invoke-Checked 'node' @('scripts/test-check-host-ports.js')
    if (Get-Command bash -ErrorAction SilentlyContinue) {
      Get-ChildItem scripts -Filter '*.sh' | ForEach-Object {
        Invoke-Checked 'bash' @('-n', ("scripts/{0}" -f $_.Name))
      }
    }
    else { Write-Host 'Bash functional/syntax validation: BLOCKED_PLATFORM' }
  }
  Run-Phase 'secretScan' {
    $forbidden = @(& git ls-files '.env' '*.xlsx' '*.log' '*tsbuildinfo' 'validation-output')
    if ($forbidden.Count -ne 0) { throw "Forbidden tracked artifacts:`n$($forbidden -join "`n")" }
    $secretMatches = @(& git grep -n -I -E '(BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|ghp_[A-Za-z0-9]{36}|AIza[A-Za-z0-9_-]{35})' -- .)
    if ($LASTEXITCODE -gt 1) { throw 'Secret scan command failed.' }
    if ($secretMatches.Count -ne 0) { throw "Possible committed secret:`n$($secretMatches -join "`n")" }
    $publicSend = @(& git grep -n -I '/api/v1/messages/send' -- 'backend/src/main/java')
    if ($LASTEXITCODE -gt 1) { throw 'Public send route scan failed.' }
    if ($publicSend.Count -ne 0) { throw 'A public real-send route exists in production source.' }
  }
  Run-Phase 'backendFormatUnitIntegrationArchitectureSecurity' { & (Join-Path $PSScriptRoot 'verify-backend-container.ps1') }
  Run-Phase 'frontendInstall' { Invoke-Checked 'npm' @('ci','--no-audit','--no-fund') (Join-Path $repoRoot 'frontend') }
  Run-Phase 'frontendTypecheck' { Invoke-Checked 'npm' @('run','typecheck') (Join-Path $repoRoot 'frontend') }
  Run-Phase 'frontendUnit' { Invoke-Checked 'npm' @('run','test:unit') (Join-Path $repoRoot 'frontend') }
  Run-Phase 'frontendBuild' { Invoke-Checked 'npm' @('run','build') (Join-Path $repoRoot 'frontend') }

  $env:POSTGRES_HOST_PORT = [string]$PostgresPort
  $env:BACKEND_HOST_PORT = [string]$BackendPort
  $env:FRONTEND_HOST_PORT = [string]$FrontendPort
  $env:CRM_BOOTSTRAP_USERNAME = 'complete-admin'
  $env:CRM_BOOTSTRAP_PASSWORD = 'complete-admin-password'
  $env:SENDING_ENABLED = 'false'
  $env:SENDING_DRY_RUN = 'true'
  $env:SENDING_DAILY_LIMIT = '0'
  $env:SENDING_KILL_SWITCH = 'true'
  $env:EMAIL_PROVIDER_MODE = 'NOOP'
  $env:WHATSAPP_PROVIDER_MODE = 'DEEPLINK_ONLY'
  $env:MESSAGING_REAL_NETWORK_ALLOWED = 'false'
  $env:OUTBOX_WORKER_ENABLED = 'false'
  $env:FAKE_INBOUND_ENABLED = 'true'
  $env:FAKE_INBOUND_WEBHOOK_SECRET = 'synthetic-complete-crm-inbound-secret'

  Run-Phase 'composeNoCacheHealthSmoke' {
    & (Join-Path $PSScriptRoot 'validate-docker-stack.ps1') -PostgresPort $PostgresPort -BackendPort $BackendPort -FrontendPort $FrontendPort -KeepRunning -NoTranscript
    $backendContainerForEnvironmentCheck = (& docker compose --profile app ps -q backend).Trim()
    $backendEnvironment = & docker inspect $backendContainerForEnvironmentCheck --format '{{json .Config.Env}}'
    Assert-ContainerEnvironmentEntries -Json $backendEnvironment -RequiredEntries @(
      'CRM_BOOTSTRAP_USERNAME=complete-admin',
      'FAKE_INBOUND_ENABLED=true',
      'OUTBOX_WORKER_ENABLED=false'
    )
  }
  $backendContainer = (& docker compose --profile app ps -q backend).Trim()
  $backendImage = (& docker inspect $backendContainer --format '{{.Config.Image}}').Trim()
  Run-Phase 'dependencyScan' {
    Invoke-Checked 'npm' @('audit','--audit-level=high') (Join-Path $repoRoot 'frontend')
    Invoke-Checked 'docker' @('volume','create','crm_grype_cache')
    Invoke-Checked 'docker' @(
      'run','--rm',
      '-v','/var/run/docker.sock:/var/run/docker.sock',
      '-v','crm_grype_cache:/root/.cache/grype',
      'anchore/grype@sha256:fd4ab4d1042b522c896e73bdf09ab8bf384fa417df99d6dd0d6e1008c7e7c821',
      $backendImage,
      '--fail-on','high'
    )
  }
  Run-Phase 'migrationFromEmpty' { & (Join-Path $PSScriptRoot 'verify-migrations.ps1') -BackendImage $backendImage }
  $summary.phases.migrationFromV11.status = $summary.phases.migrationFromEmpty.status
  $summary.phases.migrationFromV11.durationSeconds = $summary.phases.migrationFromEmpty.durationSeconds
  Run-Phase 'outboxWorkersInboundWebhook' {
    Write-Host 'Outbox, two-worker claim, lease, retry/dead-letter, signed inbound/replay/quarantine and tenant cases executed in the backend PostgreSQL suite.'
  }
  Run-Phase 'frontendE2E' {
    $env:CRM_E2E_BASE_URL = "http://127.0.0.1:$FrontendPort"
    $env:CRM_E2E_USERNAME = 'complete-admin'
    $env:CRM_E2E_PASSWORD = 'complete-admin-password'
    $env:CRM_E2E_INBOUND_SECRET = 'synthetic-complete-crm-inbound-secret'
    Invoke-Checked 'npx' @(
      'playwright','test',
      'tests/complete-crm.spec.ts',
      'tests/gmail-no-oauth.spec.ts'
    ) (Join-Path $repoRoot 'frontend')
  }
  Run-Phase 'gmailLiveFakeE2E' {
    & (Join-Path $PSScriptRoot 'validate-gmail-live-fake.ps1')
  }
  Run-Phase 'effectiveSendingBlockade' {
    $environment = & docker inspect $backendContainer --format '{{json .Config.Env}}'
    Assert-ContainerEnvironmentEntries -Json $environment -RequiredEntries @(
      'SENDING_ENABLED=false',
      'SENDING_DRY_RUN=true',
      'SENDING_DAILY_LIMIT=0',
      'SENDING_KILL_SWITCH=true',
      'MESSAGING_REAL_NETWORK_ALLOWED=false',
      'EMAIL_PROVIDER_MODE=NOOP',
      'WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY'
    )
    $postgresContainer = (& docker compose ps -q postgres).Trim()
    $database = (& docker exec $postgresContainer printenv POSTGRES_DB).Trim()
    $databaseUser = (& docker exec $postgresContainer printenv POSTGRES_USER).Trim()
    $persistent = (& docker exec $postgresContainer psql -U $databaseUser -d $database -At -c "SELECT string_agg(setting_key || '=' || setting_value, ',' ORDER BY setting_key) FROM system_setting WHERE organization_id='00000000-0000-0000-0000-000000000010' AND setting_key LIKE 'sending.%';").Trim()
    foreach ($required in @('sending.enabled=false','sending.dry-run=true','sending.daily-limit=0','sending.kill-switch=true')) { if ($persistent -notmatch [regex]::Escape($required)) { throw "Missing persistent blockade: $required" } }
  }
  Run-Phase 'zeroSent' {
    $postgresContainer = (& docker compose ps -q postgres).Trim()
    $database = (& docker exec $postgresContainer printenv POSTGRES_DB).Trim()
    $databaseUser = (& docker exec $postgresContainer printenv POSTGRES_USER).Trim()
    $sent = (& docker exec $postgresContainer psql -U $databaseUser -d $database -At -c "SELECT count(*) FROM message_record WHERE status IN ('SENT','DELIVERED','READ');").Trim()
    if ($sent -ne '0') { throw "Forbidden sent state count: $sent" }
  }
  Run-Phase 'backupRestore' { & (Join-Path $PSScriptRoot 'verify-backup-restore.ps1') }
  Run-Phase 'productionProfileSmoke' { & (Join-Path $PSScriptRoot 'verify-production-profile.ps1') -FrontendPort $ProductionFrontendPort }
  if (-not $KeepRunning) { Invoke-Checked 'docker' @('compose','--profile','app','--profile','smoke','down','-v','--remove-orphans') }
  Run-Phase 'finalTreeClean' {
    & (Join-Path $PSScriptRoot 'check-repository-safety.ps1')
    $changes = @(& git status --porcelain)
    if ($changes.Count -ne 0) { throw "Validation changed tracked files:`n$($changes -join "`n")" }
  }
  $summary.status = 'FUNCTIONAL_PASS'
} catch {
  $summary.status = 'EXECUTED_FAIL'
  $summary.error = $_.Exception.Message
  Write-Host $_.Exception.Message -ForegroundColor Red
  throw
} finally {
  if (-not $KeepRunning) {
    $cleanupPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & docker compose --profile app --profile smoke down -v --remove-orphans 2>$null | Out-Null
    $ErrorActionPreference = $cleanupPreference
  }
  $finished = (Get-Date).ToUniversalTime()
  $summary.finishedAtUtc = $finished.ToString('o')
  $summary.durationSeconds = [math]::Round(($finished - $started).TotalSeconds, 3)
  [System.IO.File]::WriteAllText($summaryPath, ($summary | ConvertTo-Json -Depth 10), [System.Text.UTF8Encoding]::new($false))
  Stop-Transcript | Out-Null
  $env:COMPOSE_PROJECT_NAME = $previousComposeProject
  Pop-Location
}
