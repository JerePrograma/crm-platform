param(
  [ValidateRange(1, 65535)]
  [int]$Port = 55432
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot '.env'

if (-not (Test-Path $envPath)) {
  throw '.env is missing. Copy .env.example to .env first.'
}

$values = @{}
Get-Content $envPath | ForEach-Object {
  $line = $_.TrimStart([char]0xFEFF)
  if ($line -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    $values[$matches.name.Trim()] = $matches.value
  }
}

$backendPort = 8080
if ($values.ContainsKey('BACKEND_HOST_PORT')) {
  $candidate = 0
  if ([int]::TryParse($values['BACKEND_HOST_PORT'], [ref]$candidate)) {
    $backendPort = $candidate
  }
}

$frontendPort = 5173
if ($values.ContainsKey('FRONTEND_HOST_PORT')) {
  $candidate = 0
  if ([int]::TryParse($values['FRONTEND_HOST_PORT'], [ref]$candidate)) {
    $frontendPort = $candidate
  }
}

& (Join-Path $PSScriptRoot 'set-local-host-ports.ps1') `
  -PostgresPort $Port `
  -BackendPort $backendPort `
  -FrontendPort $frontendPort
