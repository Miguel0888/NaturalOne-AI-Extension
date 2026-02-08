@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM Create timestamp qualifier vYYYYMMDDHHMMSS using PowerShell
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMddHHmmss"') do set TS=%%i
set QUAL=v%TS%

echo Qualifier set to: %QUAL%
echo.

REM Update all MANIFEST.MF
for /r %%f in (MANIFEST.MF) do (
  powershell -NoProfile -Command ^
    "$p='%%f';" ^
    "$c=Get-Content -LiteralPath $p -Raw -Encoding UTF8;" ^
    "$u=[regex]::Replace($c,'^(Bundle-Version:\s*)(\d+)\.(\d+)\.(\d+)(?:\.[A-Za-z0-9_-]+)?\s*$',"`$1`$2.`$3.`$4.%QUAL%",[System.Text.RegularExpressions.RegexOptions]::Multiline);" ^
    "if($u -ne $c){Set-Content -LiteralPath $p -Value $u -Encoding UTF8; Write-Host ('Updated: ' + $p)}"
)

REM Update all feature.xml (first version="..." only)
for /r %%f in (feature.xml) do (
  powershell -NoProfile -Command ^
    "$p='%%f';" ^
    "$c=Get-Content -LiteralPath $p -Raw -Encoding UTF8;" ^
    "$pat='(\bversion=\"")(\d+)\.(\d+)\.(\d+)(?:\.[A-Za-z0-9_-]+)?(\"")';" ^
    "$u=[regex]::Replace($c,$pat,('`$1`$2.`$3.`$4.%QUAL%`$6'),1);" ^
    "if($u -ne $c){Set-Content -LiteralPath $p -Value $u -Encoding UTF8; Write-Host ('Updated: ' + $p)}"
)

echo.
echo Done.
endlocal
