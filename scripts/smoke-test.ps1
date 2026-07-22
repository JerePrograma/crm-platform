$ErrorActionPreference = 'Stop'

function Fail([string]$Message) {
  throw "Smoke test failed: $Message"
}

if (-not (Test-Path '.env')) {
  Fail '.env is missing'
}

Get-Content .env | ForEach-Object {
  $line = $_.TrimStart([char]0xFEFF)
  if ($line -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    $name = $matches.name.Trim()
    $existingValue = [Environment]::GetEnvironmentVariable($name, 'Process')
    if ([string]::IsNullOrWhiteSpace($existingValue)) {
      [Environment]::SetEnvironmentVariable($name, $matches.value, 'Process')
    }
  }
}

if ([string]::IsNullOrWhiteSpace($env:CRM_BOOTSTRAP_USERNAME)) {
  Fail 'CRM_BOOTSTRAP_USERNAME is required'
}
if ([string]::IsNullOrWhiteSpace($env:CRM_BOOTSTRAP_PASSWORD)) {
  Fail 'CRM_BOOTSTRAP_PASSWORD is required'
}

$backendPort = if ($env:BACKEND_HOST_PORT) { $env:BACKEND_HOST_PORT } else { '8080' }
$frontendPort = if ($env:FRONTEND_HOST_PORT) { $env:FRONTEND_HOST_PORT } else { '5173' }
$backendUrl = if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://localhost:$backendPort" }
$frontendUrl = if ($env:FRONTEND_URL) { $env:FRONTEND_URL } else { "http://localhost:$frontendPort" }

$health = Invoke-RestMethod -Uri "$backendUrl/actuator/health"
if ($health.status -ne 'UP') {
  Fail "Backend health response is not UP: $($health.status)"
}

$session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$csrf = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/csrf" -WebSession $session
$csrfHeaders = @{}
$csrfHeaders[$csrf.headerName] = $csrf.token
$loginBody = @{
  username = $env:CRM_BOOTSTRAP_USERNAME
  password = $env:CRM_BOOTSTRAP_PASSWORD
} | ConvertTo-Json -Compress
$null = Invoke-RestMethod `
  -Uri "$backendUrl/api/v1/auth/login" `
  -Method Post `
  -WebSession $session `
  -Headers $csrfHeaders `
  -ContentType 'application/json' `
  -Body $loginBody
$prospects = Invoke-RestMethod `
  -Uri "$backendUrl/api/v1/prospects?size=1" `
  -WebSession $session
if ($null -eq $prospects.content) {
  Fail 'Authenticated prospects response does not contain a page'
}

$frontend = Invoke-WebRequest -Uri "$frontendUrl/" -UseBasicParsing
if ($frontend.Content -notmatch '<div id="root"></div>') {
  Fail 'Frontend root document was not served'
}

Write-Host 'Smoke test passed.'
Write-Host "Backend URL: $backendUrl"
Write-Host "Frontend URL: $frontendUrl"
Write-Host 'Backend health: UP'
Write-Host 'Authenticated API: reachable'
Write-Host 'Frontend: reachable'
Write-Host 'Sending controls remain configuration-only; this test performs no communications.'
