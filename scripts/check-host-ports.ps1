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

function Assert-LoopbackPortAvailable([string]$Name, [int]$Port) {
  $listener = [System.Net.Sockets.TcpListener]::new(
    [System.Net.IPAddress]::Loopback,
    $Port
  )

  try {
    $listener.Server.ExclusiveAddressUse = $true
    $listener.Start()
    Write-Host "$Name host port available: 127.0.0.1:$Port"
  } catch {
    $message = $_.Exception.Message
    throw @"
$Name host port 127.0.0.1:$Port cannot be bound.
The port may be occupied or reserved by Windows/Hyper-V.
Inspect listeners with:
  Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
Inspect excluded ranges with:
  netsh interface ipv4 show excludedportrange protocol=tcp
Choose another port and rerun the checker before starting Docker.
Underlying error: $message
"@
  } finally {
    $listener.Stop()
  }
}

Assert-LoopbackPortAvailable 'PostgreSQL' $PostgresPort
Assert-LoopbackPortAvailable 'Backend' $BackendPort
Assert-LoopbackPortAvailable 'Frontend' $FrontendPort

Write-Host 'All requested loopback host ports are available.'
