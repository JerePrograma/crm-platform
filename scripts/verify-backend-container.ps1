param(
  [string]$MavenImage = 'maven:3.9.16-eclipse-temurin-21',
  [string]$MavenCacheVolume = 'crm_maven_cache'
)

$ErrorActionPreference = 'Stop'

function Invoke-Checked([string]$Command, [string[]]$Arguments) {
  Write-Host "> $Command $($Arguments -join ' ')"
  & $Command @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw ('Command failed with exit code {0}: {1} {2}' -f $LASTEXITCODE, $Command, ($Arguments -join ' '))
  }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw 'Docker is required.'
}

$dockerApiVersion = (& docker version --format '{{.Server.APIVersion}}').Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($dockerApiVersion)) {
  throw 'Unable to determine the Docker daemon API version.'
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$targetVolume = "crm_backend_verify_target_$PID"
$containerName = "gestudio-crm-backend-verify-$PID"

Push-Location $repoRoot
try {
  Invoke-Checked 'docker' @('volume', 'create', $MavenCacheVolume)
  Invoke-Checked 'docker' @('volume', 'create', $targetVolume)

  $arguments = @(
    'run', '--rm',
    '--name', $containerName,
    '--add-host', 'host.docker.internal:host-gateway',
    '--env', 'DOCKER_HOST=unix:///var/run/docker.sock',
    '--env', 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal',
    '--env', "JAVA_TOOL_OPTIONS=-Dapi.version=$dockerApiVersion",
    '--mount', "type=bind,source=$repoRoot,target=/workspace,readonly",
    '--mount', 'type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock',
    '--mount', "type=volume,source=$MavenCacheVolume,target=/root/.m2",
    '--mount', "type=volume,source=$targetVolume,target=/workspace/backend/target",
    '--workdir', '/workspace',
    $MavenImage,
    'mvn', '-B', '-f', 'backend/pom.xml', 'verify'
  )

  Invoke-Checked 'docker' $arguments
  Write-Host 'Containerized backend verification passed.'
  Write-Host 'Covered: compilation, Spotless, unit tests, ArchUnit and Testcontainers.'
} finally {
  if (& docker ps --all --quiet --filter "name=$containerName") {
    & docker rm -f $containerName | Out-Null
  }
  if (& docker volume ls --quiet --filter "name=^$targetVolume$") {
    & docker volume rm -f $targetVolume | Out-Null
  }
  Pop-Location
}
