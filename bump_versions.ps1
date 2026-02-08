# Update OSGi bundle and feature versions by setting a timestamp qualifier.
# Run from repository root: powershell -ExecutionPolicy Bypass -File .\bump_versions.ps1

$ErrorActionPreference = "Stop"

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$qualifier = "v$timestamp"

function Normalize-Path([string]$path) {
    return $path -replace '\\','/'
}

function Update-Manifest([string]$filePath) {
    $content = Get-Content -LiteralPath $filePath -Raw -Encoding UTF8

    # Replace: Bundle-Version: X.Y.Z[.anything]  -> Bundle-Version: X.Y.Z.vYYYYMMDDHHmmss
    $updated = [regex]::Replace(
        $content,
        '^(Bundle-Version:\s*)(\d+)\.(\d+)\.(\d+)(?:\.[A-Za-z0-9_-]+)?\s*$',
        { param($m) "$($m.Groups[1].Value)$($m.Groups[2].Value).$($m.Groups[3].Value).$($m.Groups[4].Value).$qualifier" },
        [System.Text.RegularExpressions.RegexOptions]::Multiline
    )

    if ($updated -ne $content) {
        Set-Content -LiteralPath $filePath -Value $updated -Encoding UTF8
        Write-Host "Updated: $filePath"
        return $true
    }
    return $false
}

function Update-FeatureXml([string]$filePath) {
    $content = Get-Content -LiteralPath $filePath -Raw -Encoding UTF8

    # Replace first occurrence of version="X.Y.Z[.anything]" (feature's own version)
    $pattern = '(\bversion=")(\d+)\.(\d+)\.(\d+)(?:\.[A-Za-z0-9_-]+)?(")'
    $updated = [regex]::Replace(
        $content,
        $pattern,
        { param($m) "$($m.Groups[1].Value)$($m.Groups[2].Value).$($m.Groups[3].Value).$($m.Groups[4].Value).$qualifier$($m.Groups[6].Value)" },
        1
    )

    if ($updated -ne $content) {
        Set-Content -LiteralPath $filePath -Value $updated -Encoding UTF8
        Write-Host "Updated: $filePath"
        return $true
    }
    return $false
}

$changed = 0

Get-ChildItem -Path . -Recurse -File -Filter "MANIFEST.MF" | ForEach-Object {
    if (Update-Manifest $_.FullName) { $changed++ }
}

Get-ChildItem -Path . -Recurse -File -Filter "feature.xml" | ForEach-Object {
    if (Update-FeatureXml $_.FullName) { $changed++ }
}

Write-Host ""
Write-Host "Qualifier set to: $qualifier"
Write-Host "Files changed: $changed"
