param(
  [Parameter(Mandatory=$true)][string]$JavaHome,
  [string]$PostgresHome = 'C:\Program Files\PostgreSQL\18',
  [string]$NodeExecutable = (Get-Command node.exe).Source,
  [string]$OutputDirectory = (Join-Path (Split-Path -Parent $PSScriptRoot) 'validation-output\entrega-scalaris')
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$package = Join-Path ([IO.Path]::GetFullPath($OutputDirectory)) "ScalarisCRM-Windows-$stamp"
$jar = Join-Path $repo 'backend\target\crm-backend-0.1.0-SNAPSHOT.jar'
$web = Join-Path $repo 'frontend\dist'
foreach ($required in @($jar,(Join-Path $web 'index.html'),(Join-Path $JavaHome 'bin\java.exe'),$NodeExecutable,(Join-Path $PostgresHome 'bin\postgres.exe'))) {
  if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "Falta un componente compilado o un runtime: $required" }
}
if ((& (Join-Path $JavaHome 'bin\java.exe') --version | Out-String) -notmatch ' 21\.') { throw 'El paquete requiere Java 21.' }
New-Item -ItemType Directory -Path "$package\app\web","$package\runtime\java","$package\runtime\node","$package\runtime\postgres","$package\scripts" -Force | Out-Null
Copy-Item -LiteralPath $jar -Destination "$package\app\crm.jar"
Copy-Item -Path "$web\*" -Destination "$package\app\web" -Recurse
foreach ($folder in @('bin','conf','legal')) { Copy-Item -LiteralPath (Join-Path $JavaHome $folder) -Destination "$package\runtime\java" -Recurse }
New-Item -ItemType Directory -Path "$package\runtime\java\lib" -Force | Out-Null
Get-ChildItem -LiteralPath (Join-Path $JavaHome 'lib') | Where-Object { $_.Name -ne 'src.zip' } | Copy-Item -Destination "$package\runtime\java\lib" -Recurse
Copy-Item -LiteralPath (Join-Path $JavaHome 'release') -Destination "$package\runtime\java"
Copy-Item -LiteralPath $NodeExecutable -Destination "$package\runtime\node\node.exe"
$nodeLicense = Join-Path (Split-Path -Parent $NodeExecutable) 'LICENSE'
if (Test-Path -LiteralPath $nodeLicense) { Copy-Item -LiteralPath $nodeLicense -Destination "$package\runtime\node" }
foreach ($folder in @('bin','lib','share')) { Copy-Item -LiteralPath (Join-Path $PostgresHome $folder) -Destination "$package\runtime\postgres" -Recurse }
foreach ($license in @('server_license.txt','commandlinetools_3rd_party_licenses.txt')) { if (Test-Path (Join-Path $PostgresHome $license)) { Copy-Item -LiteralPath (Join-Path $PostgresHome $license) -Destination "$package\runtime\postgres" } }
foreach ($script in @('scalaris-portable.ps1','scalaris-web-server.mjs')) { Copy-Item -LiteralPath (Join-Path $PSScriptRoot $script) -Destination "$package\scripts" }
foreach ($entry in @(@('AbrirCRMScalarisPortable.cmd','Start'),@('DetenerCRMScalarisPortable.cmd','Stop'),@('RespaldarCRMScalarisPortable.cmd','Backup'))) {
  $content = "@echo off`r`npowershell.exe -NoProfile -ExecutionPolicy Bypass -File `"%~dp0scripts\scalaris-portable.ps1`" -Action $($entry[1])`r`nif errorlevel 1 pause`r`n"
  [IO.File]::WriteAllText((Join-Path $package $entry[0]), $content, (New-Object Text.ASCIIEncoding))
}
Copy-Item -LiteralPath (Join-Path $repo 'docs\scalaris-marketing-local.md') -Destination "$package\LEEME.md"
if (Test-Path (Join-Path $repo 'docs\manual-marketing-scalaris.html')) { Copy-Item -LiteralPath (Join-Path $repo 'docs\manual-marketing-scalaris.html') -Destination "$package\ManualMarketing.html" }
Copy-Item -LiteralPath (Join-Path $repo 'LICENSE') -Destination $package
$manifest = @{ createdAt=[DateTime]::UtcNow.ToString('o'); sourceCommit=(& git -C $repo rev-parse HEAD); sourceWorkingChanges=(@(& git -C $repo status --porcelain).Count -gt 0); java=((& (Join-Path $JavaHome 'bin\java.exe') --version | Out-String).Trim()); postgres=((& (Join-Path $PostgresHome 'bin\postgres.exe') --version) -join ''); node=(& $NodeExecutable --version); backendSha256=(Get-FileHash $jar).Hash; includesOperationalData=$false; sendingEnabled=$false }
$manifest.files = @(Get-ChildItem "$package\app","$package\scripts" -File -Recurse | ForEach-Object { @{ path=$_.FullName.Substring($package.Length + 1).Replace('\','/'); sha256=(Get-FileHash -LiteralPath $_.FullName).Hash } })
$manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $package 'VERSION.json') -Encoding UTF8
$zip = "$package.zip"
Compress-Archive -LiteralPath $package -DestinationPath $zip
(Get-FileHash -LiteralPath $zip).Hash | Set-Content -LiteralPath ($zip + '.sha256')
Write-Host "Carpeta portable: $package"
Write-Host "Entrega ZIP: $zip"
