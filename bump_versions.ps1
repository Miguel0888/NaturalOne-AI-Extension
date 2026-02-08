# Update OSGi bundle and feature versions by setting a timestamp qualifier.
# Run from repository root:
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\bump_versions.ps1

$ErrorActionPreference = "Stop"

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$qualifier = "v$timestamp"

function Read-TextFileUtf8([string]$filePath) {
    # Read text as UTF-8 (tolerate BOM).
    return [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)
}

function Write-TextFileUtf8NoBom([string]$filePath, [string]$content) {
    # Write text as UTF-8 without BOM.
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($filePath, $content, $utf8NoBom)
}

function Is-GeneratedBuildOutput([string]$filePath) {
    # Skip generated build output (e.g. Maven/Tycho target folders).
    return $filePath -match "[\\/]target[\\/]"
}

function Update-Manifest([string]$filePath) {
    if (Is-GeneratedBuildOutput $filePath) { return $false }

    $content = Read-TextFileUtf8 $filePath

    # Replace: Bundle-Version: X.Y.Z[.anything]  -> Bundle-Version: X.Y.Z.vYYYYMMDDHHmmss
    $updated = [regex]::Replace(
        $content,
        '^(Bundle-Version:\s*)(\d+)\.(\d+)\.(\d+)(?:\.[A-Za-z0-9_-]+)?\s*$',
        { param($m) "$($m.Groups[1].Value)$($m.Groups[2].Value).$($m.Groups[3].Value).$($m.Groups[4].Value).$qualifier" },
        [System.Text.RegularExpressions.RegexOptions]::Multiline
    )

    if ($updated -ne $content) {
        Write-TextFileUtf8NoBom -filePath $filePath -content $updated
        Write-Host "Updated: $filePath"
        return $true
    }

    return $false
}

function Update-FeatureXml([string]$filePath) {
    if (Is-GeneratedBuildOutput $filePath) { return $false }

    $content = Read-TextFileUtf8 $filePath

    # Update only the <feature ... version="..."> attribute (do not touch nested <plugin> entries).
    # Use Singleline matching because the <feature> start tag can span multiple lines.
    $pattern = '(?s)(<feature\b[^>]*?\bversion=")(\d+)\.(\d+)\.(\d+)(?:\.[A-Za-z0-9_-]+)?(")'

    $updated = [regex]::Replace(
        $content,
        $pattern,
        { param($m) "$($m.Groups[1].Value)$($m.Groups[2].Value).$($m.Groups[3].Value).$($m.Groups[4].Value).$qualifier$($m.Groups[5].Value)" },
        1
    )

    if ($updated -ne $content) {
        Write-TextFileUtf8NoBom -filePath $filePath -content $updated
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
