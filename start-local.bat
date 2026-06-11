@echo off
setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-local.ps1" %*
if errorlevel 1 (
  echo.
  echo Startup failed. Read the message above, then press any key to close.
  pause >nul
)
