@echo off
setlocal
title Scalaris CRM - Abrir
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\scalaris-local.ps1" -Action Start
if errorlevel 1 (
  echo.
  echo No se pudo abrir el CRM. Revise el mensaje de arriba.
  pause
  exit /b 1
)
endlocal
