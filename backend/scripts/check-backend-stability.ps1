param(
    [switch]$SkipEngine,
    [switch]$SkipAdmin
)

$ErrorActionPreference = "Stop"
$results = @()

function Run-TestSuite($name, $path) {
    Write-Host "==> Running clean test for $name ($path)" -ForegroundColor Cyan
    Push-Location $path
    try {
        mvn -q clean test
        Write-Host "PASS: $name" -ForegroundColor Green
        $script:results += [PSCustomObject]@{
            Service = $name
            Status = "PASS"
        }
    }
    catch {
        Write-Host "FAIL: $name" -ForegroundColor Red
        $script:results += [PSCustomObject]@{
            Service = $name
            Status = "FAIL"
        }
        throw $_
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

Write-Host ""
Write-Host "Backend stability summary:" -ForegroundColor Yellow
$results | ForEach-Object {
    Write-Host (" - {0}: {1}" -f $_.Service, $_.Status)
}
Write-Host "Backend stability check completed." -ForegroundColor Yellow
