param([ValidateRange(1, 65535)][int]$FrontendPort = 18787)

$ErrorActionPreference = 'Stop'
& node (Join-Path $PSScriptRoot 'validate-scalaris-e2e.js') --frontend-port $FrontendPort --stack scalaris
if ($LASTEXITCODE -ne 0) { throw 'Isolated Scalaris Marketing E2E failed.' }
