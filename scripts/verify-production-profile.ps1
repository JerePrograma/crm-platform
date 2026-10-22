param([ValidateRange(1, 65535)][int]$FrontendPort = 18080)

$ErrorActionPreference = 'Stop'
& node (Join-Path $PSScriptRoot 'validate-scalaris-e2e.js') --frontend-port $FrontendPort --stack production
if ($LASTEXITCODE -ne 0) { throw 'Isolated Scalaris production profile smoke failed.' }
