param(
  [Parameter(Mandatory = $true)]
  [ValidateSet('RESET_LOCAL_DATABASE')]
  [string]$ConfirmReset,

  [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Fail([string]$Message) {
  throw "Clean local initialization failed: $Message"
}

function Invoke-External([string]$Command, [string[]]$Arguments) {
  & $Command @Arguments
  if ($LASTEXITCODE -ne 0) {
    Fail "$Command $($Arguments -join ' ') exited with code $LASTEXITCODE"
  }
}

function Read-DotEnv([string]$Path) {
  $values = @{}
  foreach ($rawLine in [System.IO.File]::ReadAllLines($Path)) {
    $line = $rawLine.TrimStart([char]0xFEFF)
    if ($line -match '^(?<name>[A-Za-z_][A-Za-z0-9_]*)=(?<value>.*)$') {
      $values[$matches.name] = $matches.value
    }
  }
  return $values
}

function Set-DotEnvValues([string]$Path, [System.Collections.IDictionary]$RequiredValues) {
  $lines = @([System.IO.File]::ReadAllLines($Path))

  foreach ($entry in $RequiredValues.GetEnumerator()) {
    $key = [string]$entry.Key
    $replacement = "$key=$($entry.Value)"
    $matchingIndexes = @()

    for ($index = 0; $index -lt $lines.Count; $index++) {
      if ($lines[$index] -match "^$([regex]::Escape($key))=") {
        $matchingIndexes += $index
      }
    }

    if ($matchingIndexes.Count -gt 1) {
      Fail ".env contains duplicate entries for $key"
    }

    if ($matchingIndexes.Count -eq 1) {
      $lines[$matchingIndexes[0]] = $replacement
    } else {
      $lines += $replacement
    }
  }

  [System.IO.File]::WriteAllLines(
    $Path,
    $lines,
    [System.Text.UTF8Encoding]::new($false)
  )
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

foreach ($commandName in @('git', 'docker')) {
  if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
    Fail "Required command not found: $commandName"
  }
}

$trackedChanges = @(git status --short --untracked-files=no)
if ($LASTEXITCODE -ne 0) {
  Fail 'git status failed'
}
if ($trackedChanges.Count -gt 0) {
  $trackedChanges | ForEach-Object { Write-Host $_ }
  Fail 'tracked repository changes exist; preserve or commit them before resetting the local database'
}

$envPath = Join-Path $repoRoot '.env'
if (-not (Test-Path $envPath -PathType Leaf)) {
  Fail '.env is missing. Copy .env.example to .env and choose local credentials first'
}

$enforcedLocalValues = [ordered]@{
  SENDING_ENABLED = 'false'
  SENDING_DRY_RUN = 'true'
  SENDING_DAILY_LIMIT = '0'
  SENDING_KILL_SWITCH = 'true'
  MESSAGING_REAL_NETWORK_ALLOWED = 'false'
  EMAIL_PROVIDER_MODE = 'NOOP'
  WHATSAPP_PROVIDER_MODE = 'DEEPLINK_ONLY'
  OUTBOX_WORKER_ENABLED = 'false'
  FAKE_INBOUND_ENABLED = 'false'
  SESSION_COOKIE_SECURE = 'false'
}

Set-DotEnvValues -Path $envPath -RequiredValues $enforcedLocalValues
$envValues = Read-DotEnv -Path $envPath

foreach ($requiredName in @(
  'POSTGRES_DB',
  'DATABASE_USER',
  'DATABASE_PASSWORD',
  'BACKEND_HOST_PORT',
  'FRONTEND_HOST_PORT',
  'CRM_BOOTSTRAP_USERNAME',
  'CRM_BOOTSTRAP_PASSWORD'
)) {
  if (-not $envValues.ContainsKey($requiredName) -or [string]::IsNullOrWhiteSpace($envValues[$requiredName])) {
    Fail "$requiredName must be configured in .env"
  }
}

if ($envValues['CRM_BOOTSTRAP_PASSWORD'].Length -lt 12) {
  Fail 'CRM_BOOTSTRAP_PASSWORD must contain at least 12 characters'
}
if ($envValues['CRM_BOOTSTRAP_PASSWORD'] -eq 'change-this-local-password') {
  Fail 'replace the example CRM_BOOTSTRAP_PASSWORD before creating the clean local instance'
}

foreach ($entry in $enforcedLocalValues.GetEnumerator()) {
  if ($envValues[[string]$entry.Key] -ne [string]$entry.Value) {
    Fail "$($entry.Key) must be $($entry.Value)"
  }
}

Invoke-External 'docker' @('info')
Invoke-External 'docker' @('compose', 'version')
Invoke-External 'docker' @('compose', '--profile', 'app', '--profile', 'smoke', 'config', '--quiet')

Write-Host 'WARNING: this removes the PostgreSQL volume owned by this Docker Compose project.' -ForegroundColor Yellow
Write-Host 'The current local CRM data, including synthetic E2E records, will be deleted.' -ForegroundColor Yellow

Invoke-External 'docker' @(
  'compose',
  '--profile', 'app',
  '--profile', 'smoke',
  'down',
  '-v',
  '--remove-orphans'
)

$upArguments = @('compose', '--profile', 'app', 'up', '-d')
if (-not $NoBuild) {
  $upArguments += '--build'
}
$upArguments += '--wait'
Invoke-External 'docker' $upArguments

$backendPort = [int]$envValues['BACKEND_HOST_PORT']
$frontendPort = [int]$envValues['FRONTEND_HOST_PORT']
$backendBaseUrl = "http://127.0.0.1:$backendPort"
$frontendUrl = "http://127.0.0.1:$frontendPort"

$health = Invoke-RestMethod -Uri "$backendBaseUrl/actuator/health" -TimeoutSec 30
if ($health.status -ne 'UP') {
  Fail "backend health is not UP: $($health.status)"
}

$frontendResponse = Invoke-WebRequest -Uri $frontendUrl -UseBasicParsing -TimeoutSec 30
if ($frontendResponse.StatusCode -lt 200 -or $frontendResponse.StatusCode -ge 400) {
  Fail "frontend returned HTTP $($frontendResponse.StatusCode)"
}

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod -Uri "$backendBaseUrl/api/v1/auth/csrf" -WebSession $session -TimeoutSec 30
if ([string]::IsNullOrWhiteSpace($csrf.token) -or [string]::IsNullOrWhiteSpace($csrf.headerName)) {
  Fail 'CSRF bootstrap response is incomplete'
}

$loginHeaders = @{}
$loginHeaders[[string]$csrf.headerName] = [string]$csrf.token
$loginBody = @{
  username = $envValues['CRM_BOOTSTRAP_USERNAME']
  password = $envValues['CRM_BOOTSTRAP_PASSWORD']
} | ConvertTo-Json -Compress

Invoke-RestMethod `
  -Uri "$backendBaseUrl/api/v1/auth/login" `
  -Method Post `
  -WebSession $session `
  -Headers $loginHeaders `
  -ContentType 'application/json' `
  -Body $loginBody `
  -TimeoutSec 30 | Out-Null

$prospects = Invoke-RestMethod `
  -Uri "$backendBaseUrl/api/v1/prospects?size=1" `
  -WebSession $session `
  -TimeoutSec 30

if ([int64]$prospects.totalElements -ne 0) {
  Fail "the recreated database contains $($prospects.totalElements) prospects; expected zero"
}

Write-Host ''
Write-Host 'Clean local CRM instance is ready.' -ForegroundColor Green
Write-Host "Frontend: $frontendUrl"
Write-Host "Backend health: $backendBaseUrl/actuator/health"
Write-Host 'Business prospects: 0'
Write-Host 'Expected persistent records: Flyway schema, local organization, bootstrap administrator and login audit.'
Write-Host 'Messaging boundary: real network blocked; email NOOP; WhatsApp DEEPLINK_ONLY; workers and fake inbound disabled.'
