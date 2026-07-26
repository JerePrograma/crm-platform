$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$checker = Join-Path $PSScriptRoot 'check-host-ports.ps1'

function Get-FreeTcpPort {
  $listener = [System.Net.Sockets.TcpListener]::new(
    [System.Net.IPAddress]::Loopback,
    0
  )

  try {
    $listener.Start()
    return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
  } finally {
    $listener.Stop()
  }
}

function Get-UniqueFreePorts([int]$Count) {
  $ports = @()
  while ($ports.Count -lt $Count) {
    $candidate = Get-FreeTcpPort
    if ($ports -notcontains $candidate) {
      $ports += $candidate
    }
  }
  return $ports
}

function Invoke-Checker([int[]]$Ports, [switch]$IncludeProduction) {
  $arguments = @(
    '-NoProfile',
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    $checker,
    '-PostgresPort',
    [string]$Ports[0],
    '-BackendPort',
    [string]$Ports[1],
    '-FrontendPort',
    [string]$Ports[2]
  )

  if ($IncludeProduction) {
    $arguments += @('-ProductionFrontendPort', [string]$Ports[3])
  }

  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    $output = @(& powershell @arguments 2>&1)
    $exitCode = $LASTEXITCODE
  } finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }

  return [pscustomobject]@{
    ExitCode = $exitCode
    Output = ($output | Out-String)
  }
}

& docker ps | Out-Null
if ($LASTEXITCODE -ne 0) {
  throw 'Docker is required for the host port preflight self-test.'
}

$threePorts = Get-UniqueFreePorts 3
$compatibility = Invoke-Checker -Ports $threePorts
if ($compatibility.ExitCode -ne 0) {
  throw "Three-port compatibility expected PASS but failed:`n$($compatibility.Output)"
}

$fourPorts = Get-UniqueFreePorts 4
$fourPortPass = Invoke-Checker -Ports $fourPorts -IncludeProduction
if ($fourPortPass.ExitCode -ne 0) {
  throw "Four free ports expected PASS but failed:`n$($fourPortPass.Output)"
}

$duplicatePorts = Get-UniqueFreePorts 3
$duplicateResult = Invoke-Checker -Ports @(
  $duplicatePorts[0],
  $duplicatePorts[1],
  $duplicatePorts[2],
  $duplicatePorts[2]
) -IncludeProduction

if ($duplicateResult.ExitCode -eq 0 -or $duplicateResult.Output -notmatch 'must be different') {
  throw 'Duplicate ProductionFrontendPort expected an early uniqueness failure.'
}

$occupiedPorts = Get-UniqueFreePorts 4
$listener = [System.Net.Sockets.TcpListener]::new(
  [System.Net.IPAddress]::Loopback,
  $occupiedPorts[3]
)

try {
  $listener.Server.ExclusiveAddressUse = $true
  $listener.Start()

  $occupiedResult = Invoke-Checker -Ports $occupiedPorts -IncludeProduction
  if ($occupiedResult.ExitCode -eq 0) {
    throw 'Occupied ProductionFrontendPort expected FAIL but passed.'
  }
  if ($occupiedResult.Output -notmatch 'Production frontend host port') {
    throw "Occupied port failure did not identify Production frontend:`n$($occupiedResult.Output)"
  }
} finally {
  $listener.Stop()
}

Write-Host 'PowerShell host port preflight self-test passed.'
