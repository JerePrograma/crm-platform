function Get-DatabaseComposeArguments {
  param([string]$ComposeFile, [string]$ComposeProjectName, [string]$EnvFile)

  $arguments = @('compose')
  if ($ComposeFile) {
    $arguments += @('--file', (Resolve-Path -LiteralPath $ComposeFile -ErrorAction Stop).Path)
  }
  if ($ComposeProjectName) {
    if ($ComposeProjectName -cnotmatch '^[a-z0-9][a-z0-9_-]*$') {
      throw 'ComposeProjectName must be a valid lowercase Compose project name.'
    }
    $arguments += @('--project-name', $ComposeProjectName)
  }
  if ($EnvFile) {
    $arguments += @('--env-file', (Resolve-Path -LiteralPath $EnvFile -ErrorAction Stop).Path)
  }
  return $arguments
}

function Get-DatabaseContainer {
  param([string[]]$ComposeArguments)

  $output = @(& docker @ComposeArguments ps -q postgres)
  if ($LASTEXITCODE -ne 0) { throw 'Unable to select PostgreSQL from the requested Compose project.' }
  $containers = @($output | ForEach-Object { $_.Trim() } | Where-Object { $_ })
  if ($containers.Count -ne 1) {
    throw 'Expected exactly one running PostgreSQL container in the requested Compose project.'
  }
  return $containers[0]
}
