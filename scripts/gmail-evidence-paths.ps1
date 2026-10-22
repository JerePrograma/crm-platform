function Get-ValidatedGmailScreenshotDirectory {
  param(
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [Parameter(Mandatory = $true)][string]$ScreenshotDirectory
  )

  function Get-AbsoluteExistingDirectory([string]$Path) {
    $root = [System.IO.Path]::GetPathRoot($Path)
    if (-not [System.IO.Path]::IsPathRooted($Path) -or
        ($env:OS -eq 'Windows_NT' -and ($root -eq '\' -or $root -match '^[A-Za-z]:$'))) {
      throw 'Screenshot cleanup requires absolute filesystem paths.'
    }
    $item = Get-Item -LiteralPath ([System.IO.Path]::GetFullPath($Path)) -Force -ErrorAction Stop
    if ($item -isnot [System.IO.DirectoryInfo]) {
      throw 'Screenshot cleanup requires existing directories.'
    }
    return $item
  }

  function Assert-NoReparseAncestor([System.IO.DirectoryInfo]$Directory) {
    $current = $Directory
    while ($null -ne $current) {
      if (($current.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw 'Screenshot cleanup refuses symbolic links and reparse points.'
      }
      $current = $current.Parent
    }
  }

  $output = Get-AbsoluteExistingDirectory $OutputDirectory
  $screenshots = Get-AbsoluteExistingDirectory $ScreenshotDirectory
  Assert-NoReparseAncestor $output
  Assert-NoReparseAncestor $screenshots
  $separator = [System.IO.Path]::DirectorySeparatorChar
  $prefix = $output.FullName.TrimEnd([char[]]@('\', '/')) + $separator
  $resolvedScreenshots = $screenshots.FullName.TrimEnd([char[]]@('\', '/'))
  $comparison = [System.StringComparison]::Ordinal
  if ($env:OS -eq 'Windows_NT') { $comparison = [System.StringComparison]::OrdinalIgnoreCase }
  if (-not $resolvedScreenshots.StartsWith($prefix, $comparison)) {
    throw 'Refusing to remove a screenshot directory outside validation-output or its root.'
  }

  # Inspect each entry before descending; PowerShell 5.1 must never recurse into a junction.
  $pending = New-Object 'System.Collections.Generic.Stack[System.IO.DirectoryInfo]'
  $pending.Push($screenshots)
  while ($pending.Count -gt 0) {
    $directory = $pending.Pop()
    foreach ($entry in $directory.GetFileSystemInfos()) {
      if (($entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw 'Screenshot cleanup refuses symbolic links and reparse points.'
      }
      if (($entry.Attributes -band [System.IO.FileAttributes]::Directory) -ne 0) {
        $pending.Push([System.IO.DirectoryInfo]$entry)
      }
    }
  }
  return $resolvedScreenshots
}
