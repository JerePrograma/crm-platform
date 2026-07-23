#requires -Version 5.1

[CmdletBinding()]
param(
  [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Fail([string]$Message) {
  throw "Frontend lockfile generation failed: $Message"
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Fail 'Docker is required'
}

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$frontendPath = Join-Path $repositoryRoot 'frontend'
$packageJson = Join-Path $frontendPath 'package.json'
$currentLockfile = Join-Path $frontendPath 'package-lock.json'

if (-not (Test-Path -LiteralPath $packageJson -PathType Leaf)) {
  Fail "package.json not found at $packageJson"
}

$destinationLockfile = if ([string]::IsNullOrWhiteSpace($OutputPath)) {
  $currentLockfile
} else {
  [System.IO.Path]::GetFullPath($OutputPath)
}

$destinationDirectory = Split-Path -Parent $destinationLockfile
if ([string]::IsNullOrWhiteSpace($destinationDirectory)) {
  Fail "could not resolve output directory for $destinationLockfile"
}
New-Item -ItemType Directory -Force -Path $destinationDirectory | Out-Null

$temporaryDirectory = Join-Path `
  ([System.IO.Path]::GetTempPath()) `
  ("gestudio-frontend-lock-{0}" -f [guid]::NewGuid().ToString('N'))
$temporaryLockfile = Join-Path $temporaryDirectory 'package-lock.json'
$stagingLockfile = "$destinationLockfile.tmp-$([guid]::NewGuid().ToString('N'))"

New-Item -ItemType Directory -Force -Path $temporaryDirectory | Out-Null

try {
  Copy-Item -LiteralPath $packageJson -Destination (Join-Path $temporaryDirectory 'package.json')
  if (Test-Path -LiteralPath $currentLockfile -PathType Leaf) {
    Copy-Item -LiteralPath $currentLockfile -Destination $temporaryLockfile
  }

  & docker run --rm `
    --mount "type=bind,source=$temporaryDirectory,target=/workspace/frontend" `
    --workdir /workspace/frontend `
    node:22-alpine `
    npm install --package-lock-only --ignore-scripts --no-audit --no-fund

  if ($LASTEXITCODE -ne 0) {
    Fail "npm package-lock generation exited with code $LASTEXITCODE"
  }

  if (-not (Test-Path -LiteralPath $temporaryLockfile -PathType Leaf)) {
    Fail 'package-lock.json was not generated in the isolated workspace'
  }

  if (Test-Path -LiteralPath (Join-Path $temporaryDirectory 'node_modules')) {
    Fail 'npm created node_modules in the isolated package-lock workspace'
  }

  Copy-Item -LiteralPath $temporaryLockfile -Destination $stagingLockfile
  Move-Item -LiteralPath $stagingLockfile -Destination $destinationLockfile -Force

  $lockfileSha256 = (
    Get-FileHash -LiteralPath $destinationLockfile -Algorithm SHA256
  ).Hash.ToLowerInvariant()

  Write-Host "Frontend lockfile generated: $destinationLockfile"
  Write-Host "FRONTEND_LOCK_SHA256=$lockfileSha256"
  Write-Host 'Generation used an isolated temporary workspace.'
  Write-Host 'No package lifecycle scripts were executed.'
  Write-Host 'Any existing frontend/node_modules directory was neither read nor modified.'
} finally {
  if (Test-Path -LiteralPath $stagingLockfile) {
    Remove-Item -LiteralPath $stagingLockfile -Force
  }
  if (Test-Path -LiteralPath $temporaryDirectory) {
    Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
  }
}
