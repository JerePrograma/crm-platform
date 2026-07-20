$ErrorActionPreference = 'Stop'

function Fail([string]$Message) {
  throw "Frontend lockfile generation failed: $Message"
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Fail 'Docker is required'
}

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$frontendPath = Join-Path $repositoryRoot 'frontend'
$packageJson = Join-Path $frontendPath 'package.json'
$lockfile = Join-Path $frontendPath 'package-lock.json'

if (-not (Test-Path $packageJson)) {
  Fail "package.json not found at $packageJson"
}

& docker run --rm `
  --mount "type=bind,source=$frontendPath,target=/workspace/frontend" `
  --workdir /workspace/frontend `
  node:22-alpine `
  npm install --package-lock-only --ignore-scripts --no-audit --no-fund

if ($LASTEXITCODE -ne 0) {
  Fail "npm package-lock generation exited with code $LASTEXITCODE"
}

if (-not (Test-Path $lockfile)) {
  Fail 'package-lock.json was not generated'
}

if (Test-Path (Join-Path $frontendPath 'node_modules')) {
  Fail 'node_modules was unexpectedly created; remove it before continuing'
}

Write-Host "Frontend lockfile generated: $lockfile"
Write-Host 'No package lifecycle scripts were executed and node_modules was not created.'
Write-Host 'Review the lockfile before committing.'
