$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

. (Join-Path $PSScriptRoot 'container-env-assertions.ps1')

$requiredEntries = @(
  'SENDING_ENABLED=false',
  'SENDING_DRY_RUN=true',
  'SENDING_DAILY_LIMIT=0',
  'SENDING_KILL_SWITCH=true',
  'MESSAGING_REAL_NETWORK_ALLOWED=false',
  'EMAIL_PROVIDER_MODE=NOOP',
  'WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY'
)

function Assert-Passes {
  param([string]$Name, [scriptblock]$Action)
  try {
    & $Action
  } catch {
    throw "$Name expected PASS but failed: $($_.Exception.Message)"
  }
}

function Assert-Fails {
  param([string]$Name, [scriptblock]$Action)
  $failed = $false
  try {
    & $Action
  } catch {
    $failed = $true
  }
  if (-not $failed) {
    throw "$Name expected FAIL but passed."
  }
}

$validJson = $requiredEntries | ConvertTo-Json -Compress
Assert-Passes 'all guards present' {
  Assert-ContainerEnvironmentEntries -Json $validJson -RequiredEntries $requiredEntries
}

$missingEnabled = @($requiredEntries | Where-Object { $_ -ne 'SENDING_ENABLED=false' }) | ConvertTo-Json -Compress
Assert-Fails 'missing SENDING_ENABLED=false' {
  Assert-ContainerEnvironmentEntries -Json $missingEnabled -RequiredEntries $requiredEntries
}

$enabledTrue = @($requiredEntries | Where-Object { $_ -ne 'SENDING_ENABLED=false' }) + 'SENDING_ENABLED=true'
$enabledTrueJson = $enabledTrue | ConvertTo-Json -Compress
Assert-Fails 'SENDING_ENABLED=true' {
  Assert-ContainerEnvironmentEntries -Json $enabledTrueJson -RequiredEntries $requiredEntries
}

Assert-Fails 'invalid JSON' {
  Assert-ContainerEnvironmentEntries -Json 'not-json' -RequiredEntries $requiredEntries
}

Assert-Passes 'additional blank line' {
  Assert-ContainerEnvironmentEntries -Json @('', $validJson, '') -RequiredEntries $requiredEntries
}

Write-Host 'Container environment assertion self-test passed.'
