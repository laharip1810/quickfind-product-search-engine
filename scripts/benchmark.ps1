# Measures API response times and writes a Markdown table (PowerShell version of benchmark.sh).
#
#   .\scripts\benchmark.ps1 -Label "10k products, Redis on" -Iterations 50
param(
    [string]$Label = "run",
    [int]$Iterations = 30,
    [string]$BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" })
)
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$outDir = Join-Path $root "benchmark-results"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$out = Join-Path $outDir ((Get-Date -Format "yyyyMMdd-HHmmss") + ".md")

Invoke-RestMethod "$BaseUrl/actuator/health" | Out-Null
$page = Invoke-RestMethod "$BaseUrl/api/products?size=1&sort=popularity"
$products = $page.totalElements
$firstId = $page.content[0].id

try { docker compose -f (Join-Path $root "docker-compose.yml") exec -T redis redis-cli FLUSHALL | Out-Null; Write-Host "Flushed Redis" } catch { }

function Measure-Ms([string]$url) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-WebRequest -Uri $url -UseBasicParsing | Out-Null
    $sw.Stop()
    return [math]::Round($sw.Elapsed.TotalMilliseconds, 1)
}

$scenarios = [ordered]@{
    "Search: text query"               = "/api/products/search?query=black%20running%20shoes"
    "Search: broad query"              = "/api/products/search?query=shoes"
    "Search: filters only (SQL path)"  = "/api/products/search?category=Footwear&minPrice=2000&maxPrice=5000&sort=price_asc"
    "Search: query + filters"          = "/api/products/search?query=running&category=Footwear&minRating=4&sort=price_asc"
    "Autocomplete (cached)"            = "/api/search/suggestions?prefix=run"
    "Product detail (cached)"          = "/api/products/$firstId"
    "Recommendations (cached)"         = "/api/products/$firstId/recommendations"
    "Trending (cached)"                = "/api/products/trending"
}

$lines = @(
    "# Benchmark: $Label", "",
    "- Date: $((Get-Date).ToUniversalTime().ToString('s'))Z",
    "- Products in catalog: $products",
    "- Warm iterations per scenario: $Iterations",
    "- Base URL: $BaseUrl", "",
    "| Scenario | Cold (1st) ms | Avg ms | p50 ms | p95 ms | Max ms |",
    "|---|---:|---:|---:|---:|---:|"
)
foreach ($name in $scenarios.Keys) {
    $url = $BaseUrl + $scenarios[$name]
    $cold = Measure-Ms $url
    $samples = @(1..$Iterations | ForEach-Object { Measure-Ms $url }) | Sort-Object
    $avg = [math]::Round(($samples | Measure-Object -Average).Average, 1)
    $p50 = $samples[[int][math]::Floor(($samples.Count - 1) * 0.50)]
    $p95 = $samples[[int][math]::Floor(($samples.Count - 1) * 0.95)]
    $max = $samples[-1]
    $lines += "| $name | $cold | $avg | $p50 | $p95 | $max |"
    Write-Host "${name}: cold ${cold}ms, avg $avg, p50 $p50, p95 $p95, max $max"
}
$lines | Set-Content -Path $out -Encoding UTF8
Write-Host "`nResults written to $out"
