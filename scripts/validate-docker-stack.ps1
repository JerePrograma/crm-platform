param(
  [ValidateRange(1, 65535)]
  [int]$PostgresPort = 55432,

  [ValidateRange(1, 65535)]
  [int]$BackendPort = 8080,

  [ValidateRange(1, 65535)]
  [int]$FrontendPort = 5173,

  [switch]$KeepRunning,
  [switch]$UseBuildCache,
  [switch]$NoTranscript
)

$ErrorActionPreference = 'Stop'

function Invoke-Checked([string]$Command, [string[]]$Arguments) {
  Write-Host "> $Command $($Arguments -join ' ')"
  & $Command @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw ('Command failed with exit code {0}: {1} {2}' -f $LASTEXITCODE, $Command, ($Arguments -join ' '))
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
        return $status
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
$transcriptPath = Join-Path $outputDirectory "seg001-docker-$timestamp.log"
$summaryPath = Join-Path $outputDirectory "seg001-docker-$timestamp.json"
$transcriptStarted = $false
$startedAt = (Get-Date).ToUniversalTime()

$summary = [ordered]@{
  schemaVersion = 1
  validation = 'SEG-001 Docker stack'
  status = 'RUNNING'
  startedAtUtc = $startedAt.ToString('o')
  finishedAtUtc = $null
  commit = $null
  dockerVersion = $null
  composeVersion = $null
  ports = [ordered]@{
    postgres = $PostgresPort
    backend = $BackendPort
    frontend = $FrontendPort
  }
  hostPorts = 'NOT_RUN'
  cleanBuilds = (-not $UseBuildCache)
  services = [ordered]@{
    postgres = 'NOT_RUN'
    backend = 'NOT_RUN'
    frontend = 'NOT_RUN'
  }
  smokeHost = 'NOT_RUN'
  smokeContainer = 'NOT_RUN'
  stackKeptRunning = $false
  transcript = $(if ($NoTranscript) { $null } else { $transcriptPath })
  error = $null
}

Push-Location $repoRoot
if (-not $NoTranscript) {
  Start-Transcript -Path $transcriptPath -Force | Out-Null
  $transcriptStarted = $true
}

try {
  $summary.commit = (& git rev-parse HEAD).Trim()
  $summary.dockerVersion = (& docker --version).Trim()
  $summary.composeVersion = (& docker compose version).Trim()

  Write-Host 'SEG-001 Docker validation started.'
  Write-Host "Structured evidence: $summaryPath"
  if (-not $NoTranscript) {
    Write-Host "Transcript: $transcriptPath"
  }

  & (Join-Path $PSScriptRoot 'set-local-host-ports.ps1') `
    -PostgresPort $PostgresPort `
    -BackendPort $BackendPort `
    -FrontendPort $FrontendPort

  & (Join-Path $PSScriptRoot 'preflight.ps1') -ContainerOnly
  Invoke-Checked 'docker' @('compose', '--profile', 'app', '--profile', 'smoke', 'config', '--quiet')
  Invoke-Checked 'docker' @('compose', '--profile', 'app', '--profile', 'smoke', 'down', '--remove-orphans')

  & (Join-Path $PSScriptRoot 'check-host-ports.ps1') `
    -PostgresPort $PostgresPort `
    -BackendPort $BackendPort `
    -FrontendPort $FrontendPort
  $summary.hostPorts = 'PASS'

  $buildArguments = @('compose', '--progress', 'plain', '--profile', 'app', 'build')
  if (-not $UseBuildCache) {
    $buildArguments += '--no-cache'
  }

  Invoke-Checked 'docker' ($buildArguments + 'frontend')
  Invoke-Checked 'docker' ($buildArguments + 'backend')
  Invoke-Checked 'docker' @('compose', '--profile', 'app', 'up', '-d')

  $summary.services.postgres = Wait-ServiceHealth 'postgres'
  $summary.services.backend = Wait-ServiceHealth 'backend'
  $summary.services.frontend = Wait-ServiceHealth 'frontend'

  Invoke-Checked 'docker' @('compose', '--profile', 'app', 'ps')

  & (Join-Path $PSScriptRoot 'smoke-test.ps1')
  $summary.smokeHost = 'PASS'

  Invoke-Checked 'docker' @('compose', '--profile', 'app', '--profile', 'smoke', 'run', '--rm', 'smoke')
  $summary.smokeContainer = 'PASS'
  $summary.status = 'PASS'

  Write-Host 'SEG-001 Docker validation passed.'
  Write-Host 'Covered: host port binding, image builds, Compose config, stack health and smoke tests.'
  Write-Host 'Backend Maven verify/Testcontainers and package-lock validation remain separate controls.'
} catch {
  $summary.status = 'FAIL'
  $summary.error = $_.Exception.Message
  Write-Host 'SEG-001 Docker validation failed.' -ForegroundColor Red
  Write-Host $_.Exception.Message -ForegroundColor Red
  & docker compose --profile app --profile smoke ps
  & docker compose --profile app --profile smoke logs --no-color
  throw
} finally {
  if (-not $KeepRunning) {
    & docker compose --profile app --profile smoke down --remove-orphans
  } else {
    $runningContainers = @(& docker compose --profile app --profile smoke ps -q | Where-Object {
      -not [string]::IsNullOrWhiteSpace($_)
    })
    $summary.stackKeptRunning = ($runningContainers.Count -gt 0)
    if ($summary.stackKeptRunning) {
      Write-Host 'Stack left running because -KeepRunning was specified.'
    } else {
      Write-Host 'No running stack was available to keep after the failed validation.'
    }
  }

  $summary.finishedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
  $json = $summary | ConvertTo-Json -Depth 8
  $utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
  [System.IO.File]::WriteAllText($summaryPath, $json, $utf8NoBom)
  Write-Host "Structured evidence written: $summaryPath"

  if ($transcriptStarted) {
    Stop-Transcript | Out-Null
  }
  Pop-Location
}
