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
  -v "${frontendPath}:/workspace/frontend" `
  -w /workspace/frontend `
  node:22-alpine `
  npm install --no-audit --no-fund

if ($LASTEXITCODE -ne 0) {
  Fail "npm install exited with code $LASTEXITCODE"
}

if (-not (Test-Path $lockfile)) {
  Fail 'package-lock.json was not generated'
}

Write-Host "Frontend lockfile generated: $lockfile"
Write-Host 'Review it with git diff before committing.'
