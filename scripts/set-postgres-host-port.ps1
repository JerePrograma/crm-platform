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

$lines = Set-Or-Append $lines 'POSTGRES_HOST_PORT' $Port.ToString()
$lines = Set-Or-Append $lines 'DATABASE_URL' "jdbc:postgresql://localhost:$Port/$dbName"

$utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
[System.IO.File]::WriteAllLines($envPath, $lines, $utf8NoBom)

Write-Host 'Updated .env safely.'
Write-Host "PostgreSQL host port: $Port"
Write-Host "Database URL: jdbc:postgresql://localhost:$Port/$dbName"
Write-Host 'Existing passwords and sending controls were preserved.'
