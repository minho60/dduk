@echo off
setlocal

powershell -ExecutionPolicy Bypass -File "%~dp0start-local-runner.ps1"
if errorlevel 1 (
  echo.
  echo start-local failed. Press any key to close.
  pause >nul
)

endlocal
