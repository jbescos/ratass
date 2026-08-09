@echo off
setlocal
where powershell.exe >nul 2>nul
if errorlevel 1 (
    echo PowerShell was not found.
    exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0release-steam-windows.ps1" %*
exit /b %errorlevel%
