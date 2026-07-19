@echo off
setlocal

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

set MAVEN_VERSION=3.9.16
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%
set MAVEN_BIN=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd
set ARCHIVE=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%-bin.zip
set DISTRIBUTION_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
set EXPECTED_SHA512=ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3

if not exist "%MAVEN_BIN%" (
  if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop';" ^
    "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%ARCHIVE%';" ^
    "$actual=(Get-FileHash -Algorithm SHA512 '%ARCHIVE%').Hash.ToLowerInvariant();" ^
    "if ($actual -ne '%EXPECTED_SHA512%') { throw 'Maven SHA-512 verification failed' };" ^
    "Expand-Archive -Path '%ARCHIVE%' -DestinationPath '%MAVEN_HOME%' -Force;" ^
    "Remove-Item '%ARCHIVE%' -Force"
  if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
)

call "%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
