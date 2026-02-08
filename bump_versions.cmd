@echo off
setlocal EnableExtensions

REM Run from repository root (folder that contains this file)
pushd "%~dp0" >nul

REM Prefer PowerShell 7 (pwsh) if installed, otherwise Windows PowerShell (powershell)
set "PS_EXE=powershell"
where pwsh >nul 2>nul && set "PS_EXE=pwsh"

if not exist "%~dp0bump_versions.ps1" (
  echo ERROR: "%~dp0bump_versions.ps1" not found.
  echo        Expected it next to this .cmd.
  popd >nul
  exit /b 2
)

%PS_EXE% -NoProfile -ExecutionPolicy Bypass -File "%~dp0bump_versions.ps1"
set "RC=%ERRORLEVEL%"

popd >nul
endlocal & exit /b %RC%
