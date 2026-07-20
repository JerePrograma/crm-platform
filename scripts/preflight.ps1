param(
  [switch]$ContainerOnly
)

$ErrorActionPreference = 'Stop'

function Fail([string]$Message) {
  throw "Preflight failed: $Message"
}

foreach ($commandName in @('git', 'docker')) {
  if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
    Fail "Required command not found: $commandName"
  }
}

if (-not $ContainerOnly) {
  foreach ($commandName in @('java', 'node', 'npm')) {
    if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
      Fail "Required command not found: $commandName"
    }
  }
}

if (-not (Test-Path '.env')) {
  Fail '.env is missing. Copy .env.example to .env and edit it first'
}

Get-Content .env | ForEach-Object {
  $line = $_.TrimStart([char]0xFEFF)
  if ($line -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}

foreach ($name in @(
  'POSTGRES_DB',
  'POSTGRES_HOST_PORT',
  'DATABASE_URL',
  'DATABASE_USER',
  'DATABASE_PASSWORD',
  'CRM_BOOTSTRAP_USERNAME',
  'CRM_BOOTSTRAP_PASSWORD'
)) {
  $value = [Environment]::GetEnvironmentVariable($name, 'Process')
  if ([string]::IsNullOrWhiteSpace($value)) {
    Fail "$name is required"
  }
}

$postgresHostPort = 0
if (-not [int]::TryParse($env:POSTGRES_HOST_PORT, [ref]$postgresHostPort)) {
  Fail 'POSTGRES_HOST_PORT must be an integer'
}
if ($postgresHostPort -lt 1 -or $postgresHostPort -gt 65535) {
  Fail 'POSTGRES_HOST_PORT must be between 1 and 65535'
}
if ($env:DATABASE_URL -notmatch ":$postgresHostPort/") {
  Fail 'DATABASE_URL must use the same port as POSTGRES_HOST_PORT for host-based development'
}

if ($env:SENDING_ENABLED -ne 'false') { Fail 'SENDING_ENABLED must remain false' }
if ($env:SENDING_DRY_RUN -ne 'true') { Fail 'SENDING_DRY_RUN must remain true' }
if ($env:SENDING_DAILY_LIMIT -ne '0') { Fail 'SENDING_DAILY_LIMIT must remain 0' }
if ($env:SENDING_KILL_SWITCH -ne 'true') { Fail 'SENDING_KILL_SWITCH must remain true' }

& docker compose version | Out-Null
& docker compose --profile app config | Out-Null

Write-Host 'Preflight passed.'
Write-Host "Mode: $(if ($ContainerOnly) { 'container-only' } else { 'local-tools' })"
Write-Host "Docker: $(& docker --version)"
if (-not $ContainerOnly) {
  Write-Host "Java: $((& java -version 2>&1 | Select-Object -First 1))"
  Write-Host "Node: $(& node --version)"
  Write-Host "npm: $(& npm --version)"
}
Write-Host "PostgreSQL host port: $env:POSTGRES_HOST_PORT"
Write-Host "Database URL: $env:DATABASE_URL"
Write-Host 'Bootstrap user configured: yes'
Write-Host 'Sending controls: enabled=false dry-run=true daily-limit=0 kill-switch=true'
