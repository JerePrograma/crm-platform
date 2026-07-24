Set-StrictMode -Version 2.0

function ConvertFrom-ContainerEnvironmentJson {
  [CmdletBinding()]
  param(
    [Parameter(Mandatory = $true)]
    [AllowEmptyString()]
    [object]$Json
  )

  $jsonLines = @($Json | ForEach-Object {
    if ($null -eq $_) { '' } else { [string]$_ }
  })
  $jsonText = ($jsonLines -join [Environment]::NewLine).Trim()

  if ([string]::IsNullOrWhiteSpace($jsonText)) {
    throw 'Container environment JSON is empty.'
  }
  if (-not ($jsonText.StartsWith('[') -and $jsonText.EndsWith(']'))) {
    throw 'Container environment JSON root must be an array.'
  }

  try {
    $parsed = $jsonText | ConvertFrom-Json -ErrorAction Stop
  } catch {
    throw 'Container environment JSON is invalid.'
  }

  $entries = @($parsed | ForEach-Object { [string]$_ })
  if ($null -eq $entries) {
    throw 'Container environment JSON did not produce an enumerable result.'
  }

  return ,$entries
}

function Assert-ContainerEnvironmentEntries {
  [CmdletBinding()]
  param(
    [Parameter(Mandatory = $true)]
    [AllowEmptyString()]
    [object]$Json,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string[]]$RequiredEntries
  )

  $environmentEntries = @(ConvertFrom-ContainerEnvironmentJson -Json $Json)
  $missingEntries = @($RequiredEntries | Where-Object {
    $environmentEntries -notcontains [string]$_
  })

  if ($missingEntries.Count -gt 0) {
    throw "Missing required container environment entries: $($missingEntries -join ', ')"
  }
}
