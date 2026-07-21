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

function Assert-DockerPortNotPublished([string]$Name, [int]$Port) {
  $containers = @(& docker ps --format '{{.ID}}|{{.Names}}|{{.Ports}}')
  if ($LASTEXITCODE -ne 0) {
    throw 'Could not inspect Docker containers and their published ports.'
  }

  $portPattern = ':' + [regex]::Escape($Port.ToString()) + '->'
  $owners = @($containers | Where-Object { $_ -match $portPattern })
  if ($owners.Count -gt 0) {
    throw @"
$Name host port $Port is already published by another Docker container:
$($owners -join "`n")
Stop or reconfigure the owning container, or choose another host port.
"@
  }

  Write-Host "$Name Docker publication available: $Port"
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
Inspect Docker publications with:
  docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
Choose another port and rerun the checker before starting Docker.
Underlying error: $message
"@
  } finally {
    $listener.Stop()
  }
}

Assert-DockerPortNotPublished 'PostgreSQL' $PostgresPort
Assert-DockerPortNotPublished 'Backend' $BackendPort
Assert-DockerPortNotPublished 'Frontend' $FrontendPort

Assert-LoopbackPortAvailable 'PostgreSQL' $PostgresPort
Assert-LoopbackPortAvailable 'Backend' $BackendPort
Assert-LoopbackPortAvailable 'Frontend' $FrontendPort

Write-Host 'All requested host ports are available to Windows and not published by Docker.'
