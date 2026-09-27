# Inserts N synthetic products (on top of the 114 seed products) for benchmarking.
#
#   .\scripts\generate-test-data.ps1 -Count 10000           # against the Docker stack (default)
#   .\scripts\generate-test-data.ps1 -Count 50000 -Local    # using the packaged jar and .env
#
# Run it several times to grow the catalog: 10k -> 50k -> 100k (generated rows add up).
param(
    [int]$Count = 10000,
    [switch]$Local
)
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if ($Count -lt 1) { throw "Count must be at least 1" }
$appArgs = @("--spring.profiles.active=generate", "--quickfind.generate.count=$Count")

if ($Local) {
    $jar = "backend\target\quickfind-backend.jar"
    if (-not (Test-Path $jar)) {
        Push-Location backend
        & .\mvnw.cmd -q -DskipTests package
        Pop-Location
    }
    & java -jar $jar @appArgs
} else {
    docker compose up -d mysql redis
    docker compose run --rm --no-deps backend @appArgs
    Write-Host "Restarting the backend so it rebuilds its in-memory indexes..."
    docker compose restart backend
}
