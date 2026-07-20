$ErrorActionPreference = 'Stop'

function Fail([string]$Message) {
  throw "Repository safety scan failed: $Message"
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
  Fail 'Git is required'
}

& git diff --check
if ($LASTEXITCODE -ne 0) {
  Fail 'git diff --check reported whitespace errors'
}

$trackedFiles = @(& git ls-files)
if ($LASTEXITCODE -ne 0) {
  Fail 'git ls-files failed'
}

foreach ($path in $trackedFiles) {
  $normalized = $path -replace '\\', '/'

  if ($normalized -match '(^|/)\.env$') {
    Fail "Local environment file is tracked: $path"
  }
  if ($normalized -like 'validation-output/*') {
    Fail "Local validation evidence is tracked: $path"
  }
  if ($normalized -like 'data/import/private/*' -and $normalized -ne 'data/import/private/README.md') {
    Fail "Private import data is tracked: $path"
  }
  if ($normalized -like 'data/export/private/*') {
    Fail "Private export data is tracked: $path"
  }
  if ($normalized -match '(^|/)gestudio_lote_.*_prospectos\.xlsx$') {
    Fail "Operational prospect workbook is tracked: $path"
  }
  if ($normalized -match '\.(pem|key|p12|pfx|jks)$') {
    Fail "Key or certificate file is tracked: $path"
  }
  if ($normalized -match '(credentials|service-account|client_secret).*\.json$') {
    Fail "Credential JSON file is tracked: $path"
  }
}

Write-Host 'Repository safety scan passed.'
