#requires -Version 5.1

[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string]$EvidencePath,

  [Parameter(Mandatory = $true)]
  [string]$WorkbookPath,

  [ValidateRange(1, 65535)]
  [int]$PostgresPort = 55432,

  [ValidateRange(1, 65535)]
  [int]$BackendPort = 8080,

  [ValidateRange(1, 65535)]
  [int]$FrontendPort = 5173,

  [string]$ExpectedWorkbookSha256 = '882ca6f9b8ebb3164494ba844cc0f5be5b39584d7e4be2bc2e1478d94216e2b0',

  [ValidateRange(1, 100000)]
  [int]$ExpectedTotalRows = 422,

  [ValidateRange(1, 100000)]
  [int]$ExpectedProtectedProspectRows = 161
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Fail([string]$Message) {
  throw "Gestudio master preview resume failed: $Message"
}

function Invoke-NativeCaptured {
  [CmdletBinding()]
  param(
    [Parameter(Mandatory = $true)]
    [string]$Command,

    [string[]]$Arguments = @()
  )

  $previousErrorActionPreference = $ErrorActionPreference
  try {
    $ErrorActionPreference = 'Continue'
    $rawOutput = @(& $Command @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
  } finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }

  $output = @(
    $rawOutput | ForEach-Object {
      if ($_ -is [System.Management.Automation.ErrorRecord]) {
        if ($null -ne $_.Exception -and -not [string]::IsNullOrWhiteSpace($_.Exception.Message)) {
          [string]$_.Exception.Message
        } else {
          [string]$_
        }
      } else {
        [string]$_
      }
    }
  )

  return [pscustomobject]@{
    ExitCode = [int]$exitCode
    Output = $output
  }
}

function Invoke-Git {
  [CmdletBinding()]
  param([Parameter(Mandatory = $true)][string[]]$Arguments)

  $result = Invoke-NativeCaptured -Command 'git.exe' -Arguments $Arguments
  if ($result.ExitCode -ne 0) {
    $detail = ($result.Output | ForEach-Object { [string]$_ }) -join [Environment]::NewLine
    Fail "git $($Arguments -join ' ') exited with code $($result.ExitCode).`n$detail"
  }
  return @($result.Output)
}

function Read-DotEnv([string]$Path) {
  $values = @{}
  foreach ($rawLine in [System.IO.File]::ReadAllLines($Path)) {
    $line = $rawLine.TrimStart([char]0xFEFF).Trim()
    if ($line -eq '' -or $line.StartsWith('#')) {
      continue
    }
    if ($line -match '^(?<name>[A-Za-z_][A-Za-z0-9_]*)=(?<value>.*)$') {
      $values[$matches.name] = $matches.value
    }
  }
  return $values
}

function Require-DotEnvValue(
  [System.Collections.IDictionary]$Values,
  [string]$Name
) {
  if (-not $Values.Contains($Name) -or [string]::IsNullOrWhiteSpace([string]$Values[$Name])) {
    Fail "$Name must be configured in .env"
  }
  return [string]$Values[$Name]
}

foreach ($command in @('git.exe', 'docker.exe', 'curl.exe')) {
  if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
    Fail "required command not found in PATH: $command"
  }
}

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$resolvedRepositoryRoot = (Resolve-Path -LiteralPath $repositoryRoot).Path.TrimEnd('\')
$resolvedEvidencePath = (Resolve-Path -LiteralPath $EvidencePath).Path
$resolvedWorkbookPath = (Resolve-Path -LiteralPath $WorkbookPath).Path
$repositoryPrefix = $resolvedRepositoryRoot + '\'

if ($resolvedWorkbookPath.StartsWith(
    $repositoryPrefix,
    [System.StringComparison]::OrdinalIgnoreCase
  )) {
  Fail 'the operational workbook must remain outside the Git repository'
}

Set-Location -LiteralPath $resolvedRepositoryRoot

$previousEvidence = Get-Content -LiteralPath $resolvedEvidencePath -Raw | ConvertFrom-Json
if ($previousEvidence.validation -ne 'SEG-001 complete local validation') {
  Fail 'EvidencePath is not a SEG-001 complete validation result'
}
if ($previousEvidence.status -ne 'FAIL') {
  Fail "the supplied evidence status is $($previousEvidence.status), expected FAIL"
}
if ($previousEvidence.error -notmatch 'node_modules was unexpectedly created') {
  Fail 'the supplied evidence does not contain the supported lockfile-generation failure'
}

foreach ($phaseName in @('trackedTreeClean', 'dockerStack', 'backendMavenVerify')) {
  if ($previousEvidence.phases.$phaseName -ne 'PASS') {
    Fail "previous phase $phaseName is not PASS"
  }
}
foreach ($phaseName in @(
    'lockfileGeneration',
    'frontendNpmCiBuild',
    'finalSmokeHost',
    'finalSmokeContainer',
    'repositorySafety'
  )) {
  if ($previousEvidence.phases.$phaseName -ne 'NOT_RUN') {
    Fail "previous phase $phaseName is not NOT_RUN; refusing to repeat or overwrite evidence"
  }
}

if ([int]$previousEvidence.ports.postgres -ne $PostgresPort -or
    [int]$previousEvidence.ports.backend -ne $BackendPort -or
    [int]$previousEvidence.ports.frontend -ne $FrontendPort) {
  Fail 'the requested ports do not match the supplied validation evidence'
}

$currentBranch = (
  Invoke-Git -Arguments @('branch', '--show-current') |
  Select-Object -First 1
).ToString().Trim()
$currentHead = (
  Invoke-Git -Arguments @('rev-parse', 'HEAD') |
  Select-Object -First 1
).ToString().Trim()
$remoteHead = (
  Invoke-Git -Arguments @('rev-parse', 'origin/main') |
  Select-Object -First 1
).ToString().Trim()

if ($currentBranch -ne 'main') {
  Fail "current branch is $currentBranch, expected main"
}
if ($currentHead -ne $remoteHead) {
  Fail "local HEAD $currentHead differs from origin/main $remoteHead"
}

$status = @(Invoke-Git -Arguments @('status', '--porcelain=v1', '--untracked-files=all'))
if ($status.Count -ne 0) {
  Fail "working tree is not clean:`n$($status -join "`n")"
}

$ancestor = Invoke-NativeCaptured `
  -Command 'git.exe' `
  -Arguments @('merge-base', '--is-ancestor', [string]$previousEvidence.commit, 'HEAD')
if ($ancestor.ExitCode -ne 0) {
  Fail "previous evidence commit $($previousEvidence.commit) is not an ancestor of HEAD $currentHead"
}

$allowedChanges = @(
  'scripts/generate-frontend-lock.ps1',
  'scripts/resume-gestudio-master-preview.ps1'
)
$changesSinceEvidence = @(
  Invoke-Git -Arguments @('diff', '--name-only', "$($previousEvidence.commit)..HEAD") |
  ForEach-Object { ([string]$_).Trim() } |
  Where-Object { $_ -ne '' }
)
$unexpectedChanges = @($changesSinceEvidence | Where-Object { $_ -notin $allowedChanges })
if ($unexpectedChanges.Count -ne 0) {
  Fail "unexpected repository changes exist after the evidence commit:`n$($unexpectedChanges -join "`n")"
}

foreach ($service in @('postgres', 'backend', 'frontend')) {
  $containerId = (& docker compose --profile app ps -q $service).Trim()
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
    Fail "the previously validated Docker service is not running: $service"
  }
}

$actualWorkbookSha256 = (
  Get-FileHash -LiteralPath $resolvedWorkbookPath -Algorithm SHA256
).Hash.ToLowerInvariant()
if ($actualWorkbookSha256 -ne $ExpectedWorkbookSha256.ToLowerInvariant()) {
  Fail "workbook SHA-256 mismatch; expected $ExpectedWorkbookSha256, obtained $actualWorkbookSha256"
}

$outputDirectory = Join-Path $resolvedRepositoryRoot 'validation-output'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$summaryPath = Join-Path $outputDirectory "gestudio-master-resume-$timestamp.json"
$candidateLockfile = Join-Path $outputDirectory "frontend-package-lock-candidate-$timestamp.json"
$previewEvidencePath = Join-Path $outputDirectory "gestudio-master-preview-$timestamp.json"

$summary = [ordered]@{
  schemaVersion = 1
  operation = 'Gestudio master preview resume without repeated validations'
  status = 'RUNNING'
  startedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
  finishedAtUtc = $null
  head = $currentHead
  priorEvidence = $resolvedEvidencePath
  priorEvidenceCommit = [string]$previousEvidence.commit
  reusedPasses = @(
    'trackedTreeClean',
    'dockerStack',
    'backendMavenVerify',
    'frontend image build from unchanged tracked lockfile',
    'host smoke from unchanged image',
    'container smoke from unchanged image'
  )
  executed = [ordered]@{
    isolatedLockfileEquivalence = 'NOT_RUN'
    repositorySafety = 'NOT_RUN'
    prospectPreview = 'NOT_RUN'
  }
  lockfile = [ordered]@{
    trackedPath = 'frontend/package-lock.json'
    trackedSha256 = $null
    candidateSha256 = $null
    equivalent = $false
  }
  previewEvidence = $previewEvidencePath
  error = $null
}

try {
  & (Join-Path $PSScriptRoot 'generate-frontend-lock.ps1') -OutputPath $candidateLockfile

  $trackedLockfile = Join-Path $resolvedRepositoryRoot 'frontend\package-lock.json'
  if (-not (Test-Path -LiteralPath $trackedLockfile -PathType Leaf)) {
    Fail 'tracked frontend/package-lock.json is missing'
  }

  $summary.lockfile.trackedSha256 = (
    Get-FileHash -LiteralPath $trackedLockfile -Algorithm SHA256
  ).Hash.ToLowerInvariant()
  $summary.lockfile.candidateSha256 = (
    Get-FileHash -LiteralPath $candidateLockfile -Algorithm SHA256
  ).Hash.ToLowerInvariant()
  $summary.lockfile.equivalent = (
    $summary.lockfile.trackedSha256 -eq $summary.lockfile.candidateSha256
  )

  if (-not $summary.lockfile.equivalent) {
    Fail "generated lockfile differs from the tracked lockfile; candidate preserved at $candidateLockfile"
  }
  $summary.executed.isolatedLockfileEquivalence = 'PASS'
  Remove-Item -LiteralPath $candidateLockfile -Force

  & (Join-Path $PSScriptRoot 'check-repository-safety.ps1')
  $summary.executed.repositorySafety = 'PASS'

  $envPath = Join-Path $resolvedRepositoryRoot '.env'
  if (-not (Test-Path -LiteralPath $envPath -PathType Leaf)) {
    Fail '.env is missing'
  }
  $envValues = Read-DotEnv $envPath
  $username = Require-DotEnvValue $envValues 'CRM_BOOTSTRAP_USERNAME'
  $password = Require-DotEnvValue $envValues 'CRM_BOOTSTRAP_PASSWORD'

  $backendUrl = "http://127.0.0.1:$BackendPort"
  $session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
  $csrf = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/csrf" -WebSession $session -TimeoutSec 30
  $loginHeaders = @{}
  $loginHeaders[[string]$csrf.headerName] = [string]$csrf.token
  $loginBody = @{
    username = $username
    password = $password
  } | ConvertTo-Json -Compress

  Invoke-RestMethod `
    -Uri "$backendUrl/api/v1/auth/login" `
    -Method Post `
    -WebSession $session `
    -Headers $loginHeaders `
    -ContentType 'application/json' `
    -Body $loginBody `
    -TimeoutSec 30 | Out-Null

  $csrf = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/csrf" -WebSession $session -TimeoutSec 30
  $cookieHeader = (
    $session.Cookies.GetCookies([uri]$backendUrl) |
    ForEach-Object { "$($_.Name)=$($_.Value)" }
  ) -join '; '
  if ([string]::IsNullOrWhiteSpace($cookieHeader)) {
    Fail 'authenticated session did not produce cookies'
  }

  $previewArguments = @(
    '--fail',
    '--silent',
    '--show-error',
    '--request',
    'POST',
    "$backendUrl/api/v1/imports/prospects/preview",
    '--header',
    "$($csrf.headerName): $($csrf.token)",
    '--header',
    "Cookie: $cookieHeader",
    '--form',
    "file=@$resolvedWorkbookPath;type=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  )
  $previewResult = Invoke-NativeCaptured -Command 'curl.exe' -Arguments $previewArguments
  if ($previewResult.ExitCode -ne 0) {
    $detail = ($previewResult.Output | ForEach-Object { [string]$_ }) -join [Environment]::NewLine
    Fail "preview request failed with curl exit code $($previewResult.ExitCode).`n$detail"
  }

  $previewText = @($previewResult.Output) -join [Environment]::NewLine
  try {
    $preview = $previewText | ConvertFrom-Json
  } catch {
    Fail "preview response is not valid JSON: $previewText"
  }

  if ($preview.status -ne 'COMPLETED') {
    Fail "preview status is $($preview.status), expected COMPLETED"
  }
  if (-not [bool]$preview.dryRun) {
    Fail 'preview response is not marked as dryRun'
  }
  if ([int]$preview.totalRows -ne $ExpectedTotalRows) {
    Fail "preview processed $($preview.totalRows) rows; expected $ExpectedTotalRows"
  }
  if ([int]$preview.rejectedRows -ne 0) {
    Fail "preview contains $($preview.rejectedRows) rejected rows"
  }

  $protectedRows =
    [int]$preview.excludedRows + [int]$preview.duplicateRows + [int]$preview.reviewRows
  if ($protectedRows -lt $ExpectedProtectedProspectRows) {
    Fail "preview protects only $protectedRows rows; expected at least $ExpectedProtectedProspectRows"
  }

  $utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
  [System.IO.File]::WriteAllText($previewEvidencePath, $previewText, $utf8NoBom)
  $summary.executed.prospectPreview = 'PASS'

  $finalStatus = @(Invoke-Git -Arguments @('status', '--porcelain=v1', '--untracked-files=all'))
  if ($finalStatus.Count -ne 0) {
    Fail "working tree changed during resume:`n$($finalStatus -join "`n")"
  }

  $summary.status = 'PASS'

  Write-Host ''
  Write-Host 'RESUME_COMPLETE=true' -ForegroundColor Green
  Write-Host 'REPEATED_VALIDATIONS=0' -ForegroundColor Green
  Write-Host 'LOCKFILE_EQUIVALENT=true' -ForegroundColor Green
  Write-Host 'REPOSITORY_SAFETY=PASS' -ForegroundColor Green
  Write-Host 'PREVIEW_COMPLETE=true' -ForegroundColor Green
  Write-Host "HEAD=$currentHead"
  Write-Host "WORKBOOK_SHA256=$actualWorkbookSha256"
  Write-Host "PREVIEW_JOB_ID=$($preview.id)"
  Write-Host "PREVIEW_TOTAL_ROWS=$($preview.totalRows)"
  Write-Host "PREVIEW_ACCEPTED_ROWS=$($preview.acceptedRows)"
  Write-Host "PREVIEW_EXCLUDED_ROWS=$($preview.excludedRows)"
  Write-Host "PREVIEW_DUPLICATE_ROWS=$($preview.duplicateRows)"
  Write-Host "PREVIEW_REVIEW_ROWS=$($preview.reviewRows)"
  Write-Host "PREVIEW_REJECTED_ROWS=$($preview.rejectedRows)"
  Write-Host "PREVIEW_EVIDENCE=$previewEvidencePath"
  Write-Host 'FINAL_IMPORT_EXECUTED=false'
} catch {
  $summary.status = 'FAIL'
  $summary.error = $_.Exception.Message
  throw
} finally {
  $summary.finishedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
  $summaryJson = $summary | ConvertTo-Json -Depth 10
  $utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
  [System.IO.File]::WriteAllText($summaryPath, $summaryJson, $utf8NoBom)
  Write-Host "RESUME_EVIDENCE=$summaryPath"
}
