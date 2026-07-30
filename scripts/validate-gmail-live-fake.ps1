param(
  [ValidateRange(1, 65535)][int]$PostgresPort = 35432,
  [ValidateRange(1, 65535)][int]$BackendPort = 28080,
  [ValidateRange(1, 65535)][int]$FrontendPort = 25173,
  [ValidateRange(1, 65535)][int]$FakeGooglePort = 29090,
  [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$baseCompose = Join-Path $repoRoot 'docker-compose.yml'
$fakeCompose = Join-Path $repoRoot 'docker-compose.gmail-fake.yml'
$outputDirectory = Join-Path $repoRoot 'validation-output'
$screenshotDirectory = Join-Path $outputDirectory 'gmail-campaign-e2e/screenshots'
$manualDirectory = Join-Path $outputDirectory 'gmail-campaign-manual'
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmss')
$composeProject = "crm-gmail-fake-$PID-$stamp".ToLowerInvariant()
$transcriptPath = Join-Path $outputDirectory "gmail-live-fake-$stamp.log"
$summaryPath = Join-Path $outputDirectory "gmail-live-fake-$stamp.json"
$databaseName = 'gestudio_gmail_e2e'
$databaseUser = 'gestudio_gmail_e2e'
$organizationId = '00000000-0000-0000-0000-000000000010'
$environmentNames = @(
  'COMPOSE_PROJECT_NAME', 'POSTGRES_HOST_PORT', 'BACKEND_HOST_PORT', 'FRONTEND_HOST_PORT',
  'FAKE_GOOGLE_HOST_PORT', 'POSTGRES_DB', 'DATABASE_USER', 'DATABASE_PASSWORD',
  'CRM_BOOTSTRAP_USERNAME', 'CRM_BOOTSTRAP_PASSWORD', 'SENDING_ENABLED', 'SENDING_DRY_RUN',
  'SENDING_DAILY_LIMIT', 'SENDING_KILL_SWITCH', 'EMAIL_PROVIDER_MODE',
  'MESSAGING_REAL_NETWORK_ALLOWED', 'OUTBOX_WORKER_ENABLED', 'CRM_E2E_BASE_URL',
  'CRM_E2E_USERNAME', 'CRM_E2E_PASSWORD', 'CRM_E2E_FAKE_GOOGLE_URL',
  'CRM_E2E_FAKE_GOOGLE_CONTROL', 'CRM_E2E_POSTGRES_CONTAINER', 'CRM_E2E_COMPOSE_PROJECT',
  'CRM_E2E_DATABASE', 'CRM_E2E_DATABASE_USER', 'CRM_GMAIL_SCREENSHOTS_DIR',
  'CRM_GMAIL_MANUAL_OUTPUT'
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
  $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$summary = [ordered]@{
  schemaVersion = 1
  validation = 'Gmail OAuth and live campaign delivery against isolated fake Google'
  status = 'RUNNING'
  commit = $null
  branch = $null
  composeProject = $composeProject
  syntheticOnly = $true
  realGoogleContacted = $false
  startedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
  finishedAtUtc = $null
  ports = [ordered]@{
    postgres = $PostgresPort
    backend = $BackendPort
    frontend = $FrontendPort
    fakeGoogle = $FakeGooglePort
  }
  phases = [ordered]@{}
  evidence = [ordered]@{
    transcript = $transcriptPath
    summary = $summaryPath
    screenshots = $screenshotDirectory
    manual = $manualDirectory
  }
  error = $null
}
$phaseNames = @(
  'toolingAndPorts', 'fakeGoogleUnit', 'composeConfiguration', 'noopWithoutGoogleSecrets',
  'liveFakeStack', 'liveCampaignE2E', 'fakeProviderAssertions', 'databaseSecurityAssertions',
  'manualArtifacts', 'generatedEvidenceSafety'
)
foreach ($phaseName in $phaseNames) {
  $summary.phases[$phaseName] = [ordered]@{ status = 'NOT_RUN'; durationSeconds = $null }
}

function Invoke-Checked(
  [Parameter(Mandatory = $true)][string]$Command,
  [Parameter(Mandatory = $true)][string[]]$Arguments,
  [string]$WorkingDirectory = $repoRoot
) {
  Write-Host "> $Command $($Arguments -join ' ')"
  Push-Location $WorkingDirectory
  try {
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
      throw "Command failed with exit code ${LASTEXITCODE}: $Command"
    }
  } finally {
    Pop-Location
  }
}

function Run-Phase([string]$Name, [scriptblock]$Action) {
  Write-Host "`n=== $Name ==="
  $started = Get-Date
  $summary.phases[$Name].status = 'RUNNING'
  try {
    & $Action
    $summary.phases[$Name].status = 'FUNCTIONAL_PASS'
  } catch {
    $summary.phases[$Name].status = 'EXECUTED_FAIL'
    throw
  } finally {
    $summary.phases[$Name].durationSeconds = [Math]::Round(((Get-Date) - $started).TotalSeconds, 3)
  }
}

function Compose([string[]]$Arguments, [switch]$WithFake) {
  $files = @('-p', $composeProject, '-f', $baseCompose)
  if ($WithFake) {
    $files += @('-f', $fakeCompose)
  }
  Invoke-Checked 'docker' (@('compose') + $files + $Arguments)
}

function Wait-Healthy([string]$Service, [switch]$WithFake) {
  $files = @('-p', $composeProject, '-f', $baseCompose)
  if ($WithFake) {
    $files += @('-f', $fakeCompose)
  }
  for ($attempt = 0; $attempt -lt 90; $attempt++) {
    $containerId = (& docker compose @files ps -q $Service).Trim()
    if ($LASTEXITCODE -ne 0) {
      throw "Could not inspect Compose service $Service"
    }
    if ($containerId) {
      $health = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $containerId).Trim()
      if ($health -eq 'healthy' -or $health -eq 'running') {
        return $containerId
      }
      if ($health -eq 'unhealthy' -or $health -eq 'exited' -or $health -eq 'dead') {
        throw "Service $Service entered terminal state $health"
      }
    }
    Start-Sleep -Seconds 2
  }
  throw "Service $Service did not become healthy"
}

function Assert-PortAvailable([int]$Port) {
  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
  try {
    $listener.Start()
  } catch {
    throw "Loopback port $Port is unavailable"
  } finally {
    $listener.Stop()
  }
}

function PsqlScalar([string]$ContainerId, [string]$Sql) {
  $value = & docker exec $ContainerId psql -v ON_ERROR_STOP=1 -U $databaseUser -d $databaseName -At -c $Sql
  if ($LASTEXITCODE -ne 0) {
    throw 'PostgreSQL assertion query failed'
  }
  return ($value | Out-String).Trim()
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
if (Test-Path -LiteralPath $screenshotDirectory) {
  $resolvedOutput = [System.IO.Path]::GetFullPath($outputDirectory)
  $resolvedScreenshots = [System.IO.Path]::GetFullPath($screenshotDirectory)
  $relativeScreenshots = [System.IO.Path]::GetRelativePath($resolvedOutput, $resolvedScreenshots)
  if ($relativeScreenshots.StartsWith('..') -or [System.IO.Path]::IsPathRooted($relativeScreenshots)) {
    throw 'Refusing to remove a screenshot directory outside validation-output'
  }
  Remove-Item -LiteralPath $resolvedScreenshots -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $screenshotDirectory | Out-Null
$env:COMPOSE_PROJECT_NAME = $composeProject
$env:POSTGRES_HOST_PORT = [string]$PostgresPort
$env:BACKEND_HOST_PORT = [string]$BackendPort
$env:FRONTEND_HOST_PORT = [string]$FrontendPort
$env:FAKE_GOOGLE_HOST_PORT = [string]$FakeGooglePort
$env:POSTGRES_DB = $databaseName
$env:DATABASE_USER = $databaseUser
$env:DATABASE_PASSWORD = 'synthetic-gmail-e2e-database-password'
$env:CRM_BOOTSTRAP_USERNAME = 'gmail-e2e-admin'
$env:CRM_BOOTSTRAP_PASSWORD = 'gmail-e2e-admin-password'
$env:SENDING_ENABLED = 'false'
$env:SENDING_DRY_RUN = 'true'
$env:SENDING_DAILY_LIMIT = '0'
$env:SENDING_KILL_SWITCH = 'true'
$env:EMAIL_PROVIDER_MODE = 'NOOP'
$env:MESSAGING_REAL_NETWORK_ALLOWED = 'false'
$env:OUTBOX_WORKER_ENABLED = 'false'
$env:CRM_E2E_BASE_URL = "http://127.0.0.1:$FrontendPort"
$env:CRM_E2E_USERNAME = 'gmail-e2e-admin'
$env:CRM_E2E_PASSWORD = 'gmail-e2e-admin-password'
$env:CRM_E2E_FAKE_GOOGLE_URL = "http://127.0.0.1:$FakeGooglePort"
$env:CRM_E2E_FAKE_GOOGLE_CONTROL = 'synthetic-local-control'
$env:CRM_E2E_COMPOSE_PROJECT = $composeProject
$env:CRM_E2E_DATABASE = $databaseName
$env:CRM_E2E_DATABASE_USER = $databaseUser
$env:CRM_GMAIL_SCREENSHOTS_DIR = $screenshotDirectory
$env:CRM_GMAIL_MANUAL_OUTPUT = $manualDirectory

Push-Location $repoRoot
Start-Transcript -Path $transcriptPath -Force | Out-Null
try {
  $summary.commit = (& git rev-parse HEAD).Trim()
  $summary.branch = (& git branch --show-current).Trim()
  $detachedCiHead = $env:GITHUB_ACTIONS -eq 'true' `
    -and -not $summary.branch `
    -and $env:GITHUB_SHA `
    -and $summary.commit -eq $env:GITHUB_SHA
  if ($summary.branch -ne 'main' -and -not $detachedCiHead) {
    throw "Expected main, found $($summary.branch)"
  }

  Run-Phase 'toolingAndPorts' {
    foreach ($command in @('git', 'docker', 'node', 'npm')) {
      if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "Required command is missing: $command"
      }
    }
    Invoke-Checked 'docker' @('info')
    Invoke-Checked 'docker' @('compose', 'version')
    if (@(@($PostgresPort, $BackendPort, $FrontendPort, $FakeGooglePort) | Sort-Object -Unique).Count -ne 4) {
      throw 'All four host ports must be different'
    }
    foreach ($port in @($PostgresPort, $BackendPort, $FrontendPort, $FakeGooglePort)) {
      Assert-PortAvailable $port
    }
  }

  Run-Phase 'fakeGoogleUnit' {
    Invoke-Checked 'node' @('--test', 'scripts/fake-google-server.test.mjs')
  }

  Run-Phase 'composeConfiguration' {
    Compose @('--profile', 'app', 'config', '--quiet') -WithFake
  }

  Run-Phase 'noopWithoutGoogleSecrets' {
    Compose @('--profile', 'app', 'up', '-d', '--build', 'postgres', 'backend', 'frontend')
    [void](Wait-Healthy 'postgres')
    [void](Wait-Healthy 'backend')
    [void](Wait-Healthy 'frontend')
    Invoke-Checked 'npx' @('playwright', 'test', 'tests/gmail-no-oauth.spec.ts', '--reporter=line') (Join-Path $repoRoot 'frontend')
  }

  Run-Phase 'liveFakeStack' {
    Compose @('--profile', 'app', 'up', '-d', '--build', 'fake-google', 'backend', 'frontend') -WithFake
    [void](Wait-Healthy 'postgres' -WithFake)
    [void](Wait-Healthy 'fake-google' -WithFake)
    [void](Wait-Healthy 'backend' -WithFake)
    [void](Wait-Healthy 'frontend' -WithFake)
    $backendContainer = (& docker compose -p $composeProject -f $baseCompose -f $fakeCompose ps -q backend).Trim()
    $effective = & docker inspect --format '{{json .Config.Env}}' $backendContainer
    foreach ($required in @(
      'EMAIL_PROVIDER_MODE=GMAIL_LIVE', 'MESSAGING_REAL_NETWORK_ALLOWED=true',
      'SENDING_ENABLED=true', 'SENDING_DRY_RUN=false', 'SENDING_DAILY_LIMIT=10',
      'SENDING_KILL_SWITCH=false', 'OUTBOX_WORKER_ENABLED=false', 'GMAIL_ALLOW_TEST_ENDPOINTS=true'
    )) {
      if ($effective -notmatch [regex]::Escape($required)) {
        throw "Missing isolated live-stack environment entry: $required"
      }
    }
  }

  $postgresContainer = (& docker compose -p $composeProject -f $baseCompose -f $fakeCompose ps -q postgres).Trim()
  if (-not $postgresContainer) {
    throw 'The isolated PostgreSQL container could not be resolved'
  }
  $env:CRM_E2E_POSTGRES_CONTAINER = $postgresContainer

  Run-Phase 'liveCampaignE2E' {
    Invoke-Checked 'npx' @('playwright', 'test', 'tests/gmail-live-campaign.spec.ts', '--reporter=line') (Join-Path $repoRoot 'frontend')
  }

  Run-Phase 'fakeProviderAssertions' {
    $headers = @{ 'X-Fake-Google-Control' = 'synthetic-local-control' }
    $fakeState = Invoke-RestMethod -Uri "http://127.0.0.1:$FakeGooglePort/__fake-google__/state" -Headers $headers
    if ($fakeState.gmailRequestCount -lt 7) {
      throw "Expected at least seven individualized fake Gmail requests, got $($fakeState.gmailRequestCount)"
    }
    if ($fakeState.activeRefreshTokens -ne 0) {
      throw "Expected the synthetic refresh token to be revoked, got $($fakeState.activeRefreshTokens) active token(s)"
    }
    foreach ($request in $fakeState.gmailRequests) {
      if (-not $request.recipientHash -or -not $request.hasPlainText -or -not $request.hasHtml -or -not $request.hasOneClickUnsubscribe) {
        throw 'A fake Gmail request lacked an individualized recipient or required MIME/unsubscribe content'
      }
    }
  }

  Run-Phase 'databaseSecurityAssertions' {
    $accountEvidence = PsqlScalar $postgresContainer @"
SELECT count(*) FROM integration_connection
WHERE organization_id = '$organizationId' AND provider = 'GMAIL'
  AND status = 'REVOKED' AND encrypted_credential IS NULL
  AND credential_nonce IS NULL AND credential_key_id IS NULL;
"@
    if ($accountEvidence -ne '1') {
      throw "Expected one locally revoked Gmail account without retained credentials, got $accountEvidence"
    }
    $unsafeAddresses = PsqlScalar $postgresContainer @"
SELECT count(*) FROM contact_channel
WHERE organization_id = '$organizationId' AND type = 'EMAIL'
  AND normalized_value NOT LIKE '%@example.test';
"@
    if ($unsafeAddresses -ne '0') {
      throw "Found $unsafeAddresses non-synthetic recipient addresses in the isolated test database"
    }
    $forbiddenStatuses = PsqlScalar $postgresContainer "SELECT count(*) FROM message_record WHERE status IN ('SENT','DELIVERED','READ');"
    if ($forbiddenStatuses -ne '0') {
      throw "Found forbidden delivery claims in message_record: $forbiddenStatuses"
    }
    $ambiguous = PsqlScalar $postgresContainer "SELECT count(*) FROM message_record WHERE status = 'AMBIGUOUS' AND next_attempt_at IS NULL;"
    if ($ambiguous -lt 1) {
      throw 'No non-retried ambiguous Gmail result was persisted'
    }
    $unsubscribe = PsqlScalar $postgresContainer "SELECT count(*) FROM unsubscribe_token WHERE used_at IS NOT NULL;"
    if ($unsubscribe -lt 1) {
      throw 'The synthetic one-click unsubscribe was not persisted'
    }
  }

  Run-Phase 'manualArtifacts' {
    Invoke-Checked 'node' @('scripts/generate-gmail-campaign-manual.mjs') (Join-Path $repoRoot 'frontend')
    foreach ($artifact in @(
      'SEG-001-gmail-campaign-user-manual.html',
      'SEG-001-gmail-campaign-user-manual.pdf',
      'SEG-001-gmail-campaign-user-manual.zip',
      'index.json'
    )) {
      $path = Join-Path $manualDirectory $artifact
      if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing manual artifact: $artifact"
      }
    }
    $screenshots = @(Get-ChildItem -LiteralPath (Join-Path $manualDirectory 'png') -Filter '*.png')
    if ($screenshots.Count -ne 32) {
      throw "Expected 32 manual screenshots, found $($screenshots.Count)"
    }
    $pdfRenderer = Get-Command pdftoppm -ErrorAction SilentlyContinue
    if ($pdfRenderer) {
      & $pdfRenderer.Source -v 2>$null | Out-Null
    }
    if ($pdfRenderer -and $LASTEXITCODE -eq 0) {
      $renderPrefix = Join-Path $outputDirectory 'gmail-manual-render'
      Invoke-Checked $pdfRenderer.Source @('-f', '1', '-singlefile', '-png', '-r', '96', (Join-Path $manualDirectory 'SEG-001-gmail-campaign-user-manual.pdf'), $renderPrefix)
    }
  }

  Run-Phase 'generatedEvidenceSafety' {
    $forbidden = Get-ChildItem -LiteralPath $manualDirectory -Recurse -File -Include '*.html', '*.json' |
      Select-String -Pattern @('access_', 'refresh_', 'client_secret', 'gmail=connected', 'state=') -SimpleMatch
    if ($forbidden) {
      throw 'Generated manual evidence contains a forbidden credential or OAuth query marker'
    }
    Invoke-Checked 'git' @('diff', '--check')
    $trackedEvidence = @(& git ls-files 'validation-output')
    if ($trackedEvidence.Count -ne 0) {
      throw 'Generated validation-output evidence must not be tracked by Git'
    }
  }

  $summary.status = 'FUNCTIONAL_PASS'
} catch {
  $summary.status = 'EXECUTED_FAIL'
  $summary.error = $_.Exception.Message
  Write-Host $_.Exception.Message -ForegroundColor Red
  throw
} finally {
  if (-not $KeepRunning -and $composeProject.StartsWith('crm-gmail-fake-')) {
    $cleanupPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & docker compose -p $composeProject -f $baseCompose -f $fakeCompose --profile app down -v --remove-orphans 2>$null | Out-Null
    $ErrorActionPreference = $cleanupPreference
  }
  $summary.finishedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
  [System.IO.File]::WriteAllText(
    $summaryPath,
    ($summary | ConvertTo-Json -Depth 10),
    [System.Text.UTF8Encoding]::new($false)
  )
  Stop-Transcript | Out-Null
  foreach ($name in $environmentNames) {
    [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
  }
  Pop-Location
}
