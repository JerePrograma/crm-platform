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
$transcriptPath = Join-Path $outputDirectory "seg001-docker-$timestamp.log"

Push-Location $repoRoot
Start-Transcript -Path $transcriptPath -Force | Out-Null

try {
  Write-Host 'SEG-001 Docker validation started.'
  Write-Host "Evidence file: $transcriptPath"

  & (Join-Path $PSScriptRoot 'set-local-host-ports.ps1') `
    -PostgresPort $PostgresPort `
    -BackendPort $BackendPort `
    -FrontendPort $FrontendPort

  & (Join-Path $PSScriptRoot 'preflight.ps1') -ContainerOnly

  Invoke-Checked 'docker' @('compose', '--profile', 'app', '--profile', 'smoke', 'down', '--remove-orphans')

  $buildArguments = @('compose', '--progress', 'plain', '--profile', 'app', 'build')
  if (-not $UseBuildCache) {
    $buildArguments += '--no-cache'
  }

  Invoke-Checked 'docker' ($buildArguments + 'frontend')
  Invoke-Checked 'docker' ($buildArguments + 'backend')

  Invoke-Checked 'docker' @('compose', '--profile', 'app', 'up', '-d')

  Wait-ServiceHealth 'postgres'
  Wait-ServiceHealth 'backend'
  Wait-ServiceHealth 'frontend'

  Invoke-Checked 'docker' @('compose', '--profile', 'app', 'ps')

  & (Join-Path $PSScriptRoot 'smoke-test.ps1')
  Invoke-Checked 'docker' @('compose', '--profile', 'app', '--profile', 'smoke', 'run', '--rm', 'smoke')

  Write-Host 'SEG-001 Docker validation passed.'
  Write-Host 'This result covers clean image builds, stack health and smoke tests.'
  Write-Host 'Maven verify, Testcontainers and package-lock validation remain separate controls.'
} catch {
  Write-Host 'SEG-001 Docker validation failed.' -ForegroundColor Red
  Write-Host $_.Exception.Message -ForegroundColor Red
  & docker compose --profile app --profile smoke ps
  & docker compose --profile app --profile smoke logs --no-color
  throw
} finally {
  if (-not $KeepRunning) {
    & docker compose --profile app --profile smoke down --remove-orphans
  } else {
    Write-Host 'Stack left running because -KeepRunning was specified.'
  }
  Stop-Transcript | Out-Null
  Pop-Location
}
