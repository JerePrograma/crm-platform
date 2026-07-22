param(
  [string]$BackupDirectory,
  [string]$Database = $env:POSTGRES_DB,
  [string]$DatabaseUser = $env:DATABASE_USER
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($BackupDirectory)) {
  $BackupDirectory = Join-Path (Split-Path -Parent $repoRoot) 'crm-backups'
}
$resolvedBackup = [System.IO.Path]::GetFullPath($BackupDirectory)
$resolvedRepo = [System.IO.Path]::GetFullPath($repoRoot)
if ($resolvedBackup.StartsWith($resolvedRepo + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw 'BackupDirectory must be outside the repository.'
}
New-Item -ItemType Directory -Force -Path $resolvedBackup | Out-Null
$container = (& docker compose ps -q postgres).Trim()
if (-not $container) { throw 'The project PostgreSQL container is not running.' }
if (-not $Database) { $Database = (& docker exec $container printenv POSTGRES_DB).Trim() }
if (-not $DatabaseUser) { $DatabaseUser = (& docker exec $container printenv POSTGRES_USER).Trim() }
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
$baseName = "crm-$Database-$stamp"
$containerFile = "/tmp/$baseName.dump"
$backupFile = Join-Path $resolvedBackup "$baseName.dump"
try {
  & docker exec $container pg_dump -U $DatabaseUser -d $Database -Fc --compress=9 --no-owner --no-privileges -f $containerFile
  if ($LASTEXITCODE -ne 0) { throw 'pg_dump failed.' }
  & docker cp "${container}:${containerFile}" $backupFile
  if ($LASTEXITCODE -ne 0) { throw 'docker cp failed.' }
} finally {
  & docker exec $container rm -f $containerFile 2>$null | Out-Null
}
$checksum = (Get-FileHash -Algorithm SHA256 -LiteralPath $backupFile).Hash.ToLowerInvariant()
$postgresVersion = (& docker exec $container pg_dump --version).Trim()
$metadata = [ordered]@{
  createdAtUtc = (Get-Date).ToUniversalTime().ToString('o')
  database = $Database
  format = 'postgres-custom-compressed'
  postgresVersion = $postgresVersion
  sha256 = $checksum
  sizeBytes = (Get-Item -LiteralPath $backupFile).Length
}
$metadata | ConvertTo-Json | Set-Content -LiteralPath "$backupFile.json" -Encoding UTF8
Set-Content -LiteralPath "$backupFile.sha256" -Value "$checksum  $([System.IO.Path]::GetFileName($backupFile))" -Encoding ASCII
Write-Host "Backup created outside Git: $backupFile"
