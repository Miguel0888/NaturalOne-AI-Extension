@echo off
setlocal EnableExtensions

REM Run from repo root.
REM Ensure the PowerShell script runs from the folder where this CMD lives.
pushd "%~dp0" >nul

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bump_versions.ps1"
set EXITCODE=%ERRORLEVEL%

popd >nul
endlocal & exit /b %EXITCODE%
