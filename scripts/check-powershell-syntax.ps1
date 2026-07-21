$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$scriptFiles = Get-ChildItem -Path (Join-Path $repoRoot 'scripts') -Filter '*.ps1' -File |
  Sort-Object FullName

if ($scriptFiles.Count -eq 0) {
  throw 'No PowerShell scripts were found.'
}

$allErrors = [System.Collections.Generic.List[string]]::new()

foreach ($scriptFile in $scriptFiles) {
  $tokens = $null
  $parseErrors = $null

  [System.Management.Automation.Language.Parser]::ParseFile(
    $scriptFile.FullName,
    [ref]$tokens,
    [ref]$parseErrors
  ) | Out-Null

  if ($parseErrors -and $parseErrors.Count -gt 0) {
    foreach ($parseError in $parseErrors) {
      $allErrors.Add(
        ('{0}:{1}:{2} {3}' -f
          $scriptFile.FullName,
          $parseError.Extent.StartLineNumber,
          $parseError.Extent.StartColumnNumber,
          $parseError.Message)
      )
    }
  }
}

if ($allErrors.Count -gt 0) {
  throw "PowerShell syntax validation failed:`n$($allErrors -join "`n")"
}

Write-Host "PowerShell syntax validation passed for $($scriptFiles.Count) scripts."
