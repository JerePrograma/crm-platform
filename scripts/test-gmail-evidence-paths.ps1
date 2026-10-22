$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
. (Join-Path $PSScriptRoot 'gmail-evidence-paths.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
$evidenceRoot = Join-Path $repoRoot 'validation-output'
New-Item -ItemType Directory -Force -Path $evidenceRoot | Out-Null
$fixture = Join-Path $evidenceRoot ('gmail-evidence-paths-' + [guid]::NewGuid().ToString('N'))
$output = Join-Path $fixture 'output'
$screenshots = Join-Path $output 'gmail-campaign-e2e/screenshots'
$outside = Join-Path $fixture 'outside'
$sibling = Join-Path $fixture 'output-sibling'
$links = New-Object 'System.Collections.Generic.List[string]'
$script:checks = 0

function Assert-True([bool]$Condition, [string]$Name) {
  if (-not $Condition) { throw "Assertion failed: $Name" }
  $script:checks++
}

function Assert-Rejected([string]$Name, [string]$Base, [string]$Target) {
  $rejected = $false
  try { [void](Get-ValidatedGmailScreenshotDirectory -OutputDirectory $Base -ScreenshotDirectory $Target) }
  catch { $rejected = $true }
  Assert-True $rejected $Name
}

function Add-DirectoryLink([string]$Link, [string]$Target) {
  $kind = 'SymbolicLink'
  if ($env:OS -eq 'Windows_NT') { $kind = 'Junction' }
  New-Item -ItemType $kind -Path $Link -Target $Target | Out-Null
  $links.Add($Link)
}

try {
  foreach ($directory in @($screenshots, $outside, $sibling, (Join-Path $outside 'screenshots'))) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
  }
  $sentinel = Join-Path $outside 'preserve.txt'
  [System.IO.File]::WriteAllText($sentinel, 'synthetic external sentinel')
  $image = Join-Path $screenshots 'synthetic.png'
  [System.IO.File]::WriteAllText($image, 'synthetic screenshot')

  $validated = Get-ValidatedGmailScreenshotDirectory -OutputDirectory $output -ScreenshotDirectory $screenshots
  Assert-True ($validated -eq [System.IO.Path]::GetFullPath($screenshots)) 'existing strict descendant'
  Assert-Rejected 'output root' $output $output
  Assert-Rejected 'output root with trailing separator' $output ($output + [System.IO.Path]::DirectorySeparatorChar)
  Assert-Rejected 'parent directory' $output $fixture
  Assert-Rejected 'sibling with matching prefix' $output $sibling
  Assert-Rejected 'relative output' 'validation-output' $screenshots
  Assert-Rejected 'relative screenshots' $output 'gmail-campaign-e2e/screenshots'
  Assert-Rejected 'missing directory' $output (Join-Path $output 'missing')
  Assert-Rejected 'file instead of directory' $output $image
  $normalized = Get-ValidatedGmailScreenshotDirectory -OutputDirectory $output -ScreenshotDirectory (Join-Path $screenshots '../screenshots')
  Assert-True ($normalized -eq $validated) 'normalized path stays within output'

  $targetLink = Join-Path $output 'linked-screenshots'
  Add-DirectoryLink $targetLink $outside
  Assert-Rejected 'target junction or symbolic link' $output $targetLink
  Assert-Rejected 'ancestor junction or symbolic link' $output (Join-Path $targetLink 'screenshots')
  $childLink = Join-Path $screenshots 'linked-child'
  Add-DirectoryLink $childLink $outside
  Assert-Rejected 'descendant junction or symbolic link' $output $screenshots
  $outputLink = Join-Path $fixture 'linked-output'
  Add-DirectoryLink $outputLink $outside
  Assert-Rejected 'output junction or symbolic link' $outputLink (Join-Path $outputLink 'screenshots')
  foreach ($link in $links) { (Get-Item -LiteralPath $link -Force).Delete() }
  $links.Clear()
  Assert-True ([System.IO.File]::ReadAllText($sentinel) -eq 'synthetic external sentinel') 'external sentinel preserved'

  # Import only these function definitions; do not run the Gmail wrapper or Docker.
  $tokens = $null; $parseErrors = $null
  $wrapperAst = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'validate-gmail-live-fake.ps1'), [ref]$tokens, [ref]$parseErrors)
  if ($parseErrors.Count) { throw 'Gmail wrapper has a PowerShell syntax error.' }
  foreach ($functionName in @('Test-PdfRendererVersion', 'Invoke-Checked')) {
    $definition = $wrapperAst.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $functionName }, $true)
    if ($null -eq $definition) { throw "Missing wrapper function: $functionName" }
    . ([scriptblock]::Create($definition.Extent.Text))
  }
  $hostName = 'pwsh'
  if ($env:OS -eq 'Windows_NT') {
    $hostName = 'pwsh.exe'
    if ($PSVersionTable.PSVersion.Major -eq 5) { $hostName = 'powershell.exe' }
  }
  $hostExecutable = Join-Path $PSHOME $hostName
  $successProbe = Join-Path $fixture 'synthetic-version-success.ps1'
  $failureProbe = Join-Path $fixture 'synthetic-version-failure.ps1'
  [System.IO.File]::WriteAllText($successProbe, "[Console]::Error.WriteLine('synthetic renderer version'); exit 0")
  [System.IO.File]::WriteAllText($failureProbe, "[Console]::Error.WriteLine('synthetic renderer failure'); exit 7")
  Assert-True (Test-PdfRendererVersion $hostExecutable @('-NoProfile', '-File', $successProbe)) 'stderr version with exit zero succeeds'
  Assert-True ($ErrorActionPreference -eq 'Stop') 'success probe preserves error preference'
  Assert-True (-not (Test-PdfRendererVersion $hostExecutable @('-NoProfile', '-File', $failureProbe))) 'nonzero version probe fails'
  Assert-True ($ErrorActionPreference -eq 'Stop') 'failed probe preserves error preference'
  $renderFailed = $false
  try { Invoke-Checked $hostExecutable @('-NoProfile', '-File', $failureProbe) $fixture }
  catch { $renderFailed = $true }
  Assert-True $renderFailed 'real renderer errors remain fatal'
  Assert-True ($ErrorActionPreference -eq 'Stop') 'real renderer failure preserves error preference'

  $validated = Get-ValidatedGmailScreenshotDirectory -OutputDirectory $output -ScreenshotDirectory $screenshots
  Remove-Item -LiteralPath $validated -Recurse -Force
  Assert-True (-not (Test-Path -LiteralPath $screenshots)) 'validated screenshot directory removed'
  Assert-True (Test-Path -LiteralPath $sentinel -PathType Leaf) 'external file survives actual cleanup'
  Write-Host "Gmail evidence paths: PASS $script:checks assertions; PowerShell $($PSVersionTable.PSVersion)."
} finally {
  # Delete only fixture links themselves, never their targets, before validating cleanup.
  foreach ($link in $links) {
    if (Test-Path -LiteralPath $link) { (Get-Item -LiteralPath $link -Force).Delete() }
  }
  if (Test-Path -LiteralPath $fixture) {
    $validatedFixture = Get-ValidatedGmailScreenshotDirectory -OutputDirectory $evidenceRoot -ScreenshotDirectory $fixture
    Remove-Item -LiteralPath $validatedFixture -Recurse -Force
  }
}

# Expected native failures must not escape a successful assertions and cleanup run.
$global:LASTEXITCODE = 0
