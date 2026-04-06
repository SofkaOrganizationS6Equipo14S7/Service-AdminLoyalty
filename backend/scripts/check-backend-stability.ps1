param(
    [switch]$SkipEngine,
    [switch]$SkipAdmin
)

$ErrorActionPreference = "Stop"

function Run-TestSuite($name, $path) {
    Write-Host "==> Running clean test for $name ($path)" -ForegroundColor Cyan
    Push-Location $path
    try {
        mvn -q clean test
        Write-Host "PASS: $name" -ForegroundColor Green
    }
    catch {
        Write-Host "FAIL: $name" -ForegroundColor Red
        throw
    }
    finally {
        Pop-Location
    }
}

$root = Split-Path -Parent $PSScriptRoot

if (-not $SkipEngine) {
    Run-TestSuite "service-engine" (Join-Path $root "service-engine")
}

if (-not $SkipAdmin) {
    Run-TestSuite "service-admin" (Join-Path $root "service-admin")
}

Write-Host "Backend stability check completed." -ForegroundColor Yellow

