@echo off
setlocal
title Scalaris CRM - Detener
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\scalaris-local.ps1" -Action Stop
if errorlevel 1 (
  echo.
  echo No se pudo detener el CRM. Revise el mensaje de arriba.
  pause
  exit /b 1
)
echo.
pause
endlocal
