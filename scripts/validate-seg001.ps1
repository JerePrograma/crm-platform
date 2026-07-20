param(
  [ValidateRange(1, 65535)]
  [int]$PostgresPort = 55432,

  [ValidateRange(1, 65535)]
  [int]$BackendPort = 8080,

  [ValidateRange(1, 65535)]
  [int]$FrontendPort = 5173,

  [switch]$KeepRunning,
  [switch]$UseBuildCache
)

$ErrorActionPreference = 'Stop'

function Invoke-Checked([string]$Command, [string[]]$Arguments) {
  Write-Host "> $Command $($Arguments -join ' ')"
  & $Command @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed with exit code $LASTEXITCODE: $Command $($Arguments -join ' ')"
  }
}

function Wait-ServiceHealth([string]$Service, [int]$Attempts = 60) {
  for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
    $containerId = (& docker compose --profile app ps -q $Service).Trim()
    if ($LASTEXITCODE -ne 0) {
      throw "Could not resolve container for service $Service"
    }
    if (-not [string]::IsNullOrWhiteSpace($containerId)) {
      $status = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $containerId).Trim()
      if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect service $Service"
      }
      Write-Host "$Service health: $status"
      if ($status -eq 'healthy') {
        return
      }
      if ($status -in @('exited', 'dead', 'unhealthy')) {
        throw "$Service entered terminal state: $status"
      }
    }
    Start-Sleep -Seconds 5
  }
  throw "Timed out waiting for $Service to become healthy"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $repoRoot 'validation-output'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$transcriptPath = Join-Path $outputDirectory "seg001-complete-$timestamp.log"
$summaryPath = Join-Path $outputDirectory "seg001-complete-$timestamp.json"
$startedAt = (Get-Date).ToUniversalTime()
$transcriptStarted = $false

$summary = [ordered]@{
  schemaVersion = 1
  validation = 'SEG-001 complete local validation'
  status = 'RUNNING'
  startedAtUtc = $startedAt.ToString('o')
  finishedAtUtc = $null
  commit = $null
  branch = $null
  ports = [ordered]@{
    postgres = $PostgresPort
    backend = $BackendPort
    frontend = $FrontendPort
  }
  phases = [ordered]@{
    trackedTreeClean = 'NOT_RUN'
    dockerStack = 'NOT_RUN'
    backendMavenVerify = 'NOT_RUN'
    lockfileGeneration = 'NOT_RUN'
    frontendNpmCiBuild = 'NOT_RUN'
    finalSmokeHost = 'NOT_RUN'
    finalSmokeContainer = 'NOT_RUN'
    repositorySafety = 'NOT_RUN'
  }
  lockfile = [ordered]@{
    path = 'frontend/package-lock.json'
    sha256 = $null
  }
  dockerEvidence = $null
  transcript = $transcriptPath
  stackKeptRunning = $false
  error = $null
}

Push-Location $repoRoot
Start-Transcript -Path $transcriptPath -Force | Out-Null
$transcriptStarted = $true

try {
  $summary.commit = (& git rev-parse HEAD).Trim()
  $summary.branch = (& git rev-parse --abbrev-ref HEAD).Trim()

  if ($summary.branch -ne 'main') {
    throw "Validation must run from main; current branch is $($summary.branch)"
  }

  $trackedChanges = @(& git status --porcelain --untracked-files=no)
  if ($trackedChanges.Count -gt 0) {
    throw "Tracked working tree is not clean. Review or restore changes before validation:`n$($trackedChanges -join "`n")"
  }
  $summary.phases.trackedTreeClean = 'PASS'

  Write-Host 'Complete SEG-001 validation started.'
  Write-Host "Commit: $($summary.commit)"
  Write-Host "Transcript: $transcriptPath"
  Write-Host "Structured evidence: $summaryPath"

  $dockerParameters = @{
    PostgresPort = $PostgresPort
    BackendPort = $BackendPort
    FrontendPort = $FrontendPort
    KeepRunning = $true
    NoTranscript = $true
  }
  if ($UseBuildCache) {
    $dockerParameters.UseBuildCache = $true
  }
  & (Join-Path $PSScriptRoot 'validate-docker-stack.ps1') @dockerParameters
  $summary.phases.dockerStack = 'PASS'

  $dockerEvidence = Get-ChildItem -Path $outputDirectory -Filter 'seg001-docker-*.json' |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1
  if ($dockerEvidence) {
    $summary.dockerEvidence = $dockerEvidence.FullName
  }

  & (Join-Path $PSScriptRoot 'verify-backend-container.ps1')
  $summary.phases.backendMavenVerify = 'PASS'

  & (Join-Path $PSScriptRoot 'generate-frontend-lock.ps1')
  $lockfilePath = Join-Path $repoRoot 'frontend/package-lock.json'
  if (-not (Test-Path $lockfilePath)) {
    throw 'frontend/package-lock.json was not generated'
  }
  $summary.lockfile.sha256 = (Get-FileHash -Algorithm SHA256 -Path $lockfilePath).Hash.ToLowerInvariant()
  $summary.phases.lockfileGeneration = 'PASS'

  $frontendBuildArguments = @('compose', '--progress', 'plain', '--profile', 'app', 'build')
  if (-not $UseBuildCache) {
    $frontendBuildArguments += '--no-cache'
  }
  $frontendBuildArguments += 'frontend'
  Invoke-Checked 'docker' $frontendBuildArguments
  $summary.phases.frontendNpmCiBuild = 'PASS'

  Invoke-Checked 'docker' @('compose', '--profile', 'app', 'up', '-d', '--no-deps', '--force-recreate', 'frontend')
  Wait-ServiceHealth 'frontend'

  & (Join-Path $PSScriptRoot 'smoke-test.ps1')
  $summary.phases.finalSmokeHost = 'PASS'

  Invoke-Checked 'docker' @('compose', '--profile', 'app', '--profile', 'smoke', 'run', '--rm', 'smoke')
  $summary.phases.finalSmokeContainer = 'PASS'

  Invoke-Checked 'git' @('diff', '--check')
  $trackedFiles = @(& git ls-files)
  $forbiddenTracked = @($trackedFiles | Where-Object {
    $_ -eq '.env' -or
    $_ -like 'validation-output/*' -or
    $_ -match '^gestudio_lote_.*_prospectos\.xlsx$'
  })
  if ($forbiddenTracked.Count -gt 0) {
    throw "Forbidden local or operational files are tracked:`n$($forbiddenTracked -join "`n")"
  }

  $unexpectedChanges = @(& git status --porcelain | Where-Object {
    $_ -notmatch '^\?\? frontend/package-lock\.json$' -and
    $_ -notmatch '^ M frontend/package-lock\.json$' -and
    $_ -notmatch '^M  frontend/package-lock\.json$'
  })
  if ($unexpectedChanges.Count -gt 0) {
    throw "Unexpected working tree changes after validation:`n$($unexpectedChanges -join "`n")"
  }
  $summary.phases.repositorySafety = 'PASS'

  $summary.status = 'PASS'
  Write-Host 'Complete SEG-001 validation passed.'
  Write-Host 'The generated package-lock.json remains uncommitted for review.'
} catch {
  $summary.status = 'FAIL'
  $summary.error = $_.Exception.Message
  Write-Host 'Complete SEG-001 validation failed.' -ForegroundColor Red
  Write-Host $_.Exception.Message -ForegroundColor Red
  & docker compose --profile app --profile smoke ps
  & docker compose --profile app --profile smoke logs --no-color
  throw
} finally {
  if (-not $KeepRunning) {
    & docker compose --profile app --profile smoke down --remove-orphans
  } else {
    $summary.stackKeptRunning = $true
    Write-Host 'Stack left running because -KeepRunning was specified.'
  }

  $summary.finishedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
  $json = $summary | ConvertTo-Json -Depth 10
  $utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
  [System.IO.File]::WriteAllText($summaryPath, $json, $utf8NoBom)
  Write-Host "Structured evidence written: $summaryPath"

  if ($transcriptStarted) {
    Stop-Transcript | Out-Null
  }
  Pop-Location
}
