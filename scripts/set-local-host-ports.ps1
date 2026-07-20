param(
  [ValidateRange(1, 65535)]
  [int]$PostgresPort = 55432,

  [ValidateRange(1, 65535)]
  [int]$BackendPort = 8080,

  [ValidateRange(1, 65535)]
  [int]$FrontendPort = 5173
)

$ErrorActionPreference = 'Stop'

$ports = @($PostgresPort, $BackendPort, $FrontendPort)
if (($ports | Select-Object -Unique).Count -ne $ports.Count) {
  throw 'PostgresPort, BackendPort and FrontendPort must be different.'
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot '.env'

if (-not (Test-Path $envPath)) {
  throw '.env is missing. Copy .env.example to .env first.'
}

$lines = @(Get-Content $envPath | ForEach-Object { $_.TrimStart([char]0xFEFF) })
$dbName = 'gestudio_crm'
$dbLine = $lines | Where-Object { $_ -match '^POSTGRES_DB=' } | Select-Object -First 1
if ($dbLine) {
  $candidate = ($dbLine -split '=', 2)[1].Trim()
  if (-not [string]::IsNullOrWhiteSpace($candidate)) {
    $dbName = $candidate
  }
}

function Set-Or-Append([string[]]$InputLines, [string]$Name, [string]$Value) {
  $pattern = "^$([Regex]::Escape($Name))="
  $found = $false
  $result = foreach ($line in $InputLines) {
    if ($line -match $pattern) {
      $found = $true
      "$Name=$Value"
    } else {
      $line
    }
  }
  if (-not $found) {
    $result += "$Name=$Value"
  }
  return @($result)
}

$lines = Set-Or-Append $lines 'POSTGRES_HOST_PORT' $PostgresPort.ToString()
$lines = Set-Or-Append $lines 'BACKEND_HOST_PORT' $BackendPort.ToString()
$lines = Set-Or-Append $lines 'FRONTEND_HOST_PORT' $FrontendPort.ToString()
$lines = Set-Or-Append $lines 'DATABASE_URL' "jdbc:postgresql://localhost:$PostgresPort/$dbName"

$utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
[System.IO.File]::WriteAllLines($envPath, $lines, $utf8NoBom)

Write-Host 'Updated .env safely.'
Write-Host "PostgreSQL host port: $PostgresPort"
Write-Host "Backend host port: $BackendPort"
Write-Host "Frontend host port: $FrontendPort"
Write-Host "Database URL: jdbc:postgresql://localhost:$PostgresPort/$dbName"
Write-Host 'Existing passwords and sending controls were preserved.'
