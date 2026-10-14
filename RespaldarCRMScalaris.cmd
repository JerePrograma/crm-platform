@echo off
setlocal
title Scalaris CRM - Respaldo
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\scalaris-local.ps1" -Action Backup
if errorlevel 1 (
  echo.
  echo No se pudo guardar el respaldo. Revise el mensaje de arriba.
  pause
  exit /b 1
)
echo.
pause
endlocal
