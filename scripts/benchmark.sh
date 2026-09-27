#!/usr/bin/env bash
# Measures API response times with curl and writes a Markdown table.
#
#   scripts/benchmark.sh [label] [iterations]
#   scripts/benchmark.sh "10k products, V2 indexes, Redis on" 50
#
# Environment: BASE_URL (default http://localhost:8080)
# For each scenario it records the first (cold) request separately, then runs N warm
# requests and reports average, p50, p95 and max in milliseconds. Before the run the
# Redis cache is flushed (if the Docker stack is running) so "cold" really is a cache miss.
set -euo pipefail

LABEL="${1:-run}"
N="${2:-30}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/benchmark-results"
mkdir -p "$OUT_DIR"
OUT="$OUT_DIR/$(date +%Y%m%d-%H%M%S).md"

curl -fs "$BASE_URL/actuator/health" > /dev/null || { echo "Backend not reachable at $BASE_URL" >&2; exit 1; }
PRODUCTS=$(curl -fs "$BASE_URL/api/products?size=1" | sed -E 's/.*"totalElements":([0-9]+).*/\1/')
FIRST_ID=$(curl -fs "$BASE_URL/api/products?size=1&sort=popularity" | sed -E 's/.*"content":\[\{"id":([0-9]+).*/\1/')

if docker compose -f "$ROOT/docker-compose.yml" ps redis 2>/dev/null | grep -q redis; then
  docker compose -f "$ROOT/docker-compose.yml" exec -T redis redis-cli FLUSHALL > /dev/null && echo "Flushed Redis"
fi

time_ms() { curl -s -o /dev/null -w "%{time_total}" "$1" | awk '{printf "%.1f", $1 * 1000}'; }

declare -a SCENARIOS=(
  "Search: text query|/api/products/search?query=black%20running%20shoes"
  "Search: broad query|/api/products/search?query=shoes"
  "Search: filters only (SQL path)|/api/products/search?category=Footwear&minPrice=2000&maxPrice=5000&sort=price_asc"
  "Search: query + filters|/api/products/search?query=running&category=Footwear&minRating=4&sort=price_asc"
  "Autocomplete (cached)|/api/search/suggestions?prefix=run"
  "Product detail (cached)|/api/products/$FIRST_ID"
  "Recommendations (cached)|/api/products/$FIRST_ID/recommendations"
  "Trending (cached)|/api/products/trending"
)

{
  echo "# Benchmark: $LABEL"
  echo
  echo "- Date: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "- Products in catalog: $PRODUCTS"
  echo "- Warm iterations per scenario: $N"
  echo "- Base URL: $BASE_URL"
  echo
  echo "| Scenario | Cold (1st) ms | Avg ms | p50 ms | p95 ms | Max ms |"
  echo "|---|---:|---:|---:|---:|---:|"
} > "$OUT"

for entry in "${SCENARIOS[@]}"; do
  name="${entry%%|*}"
  path="${entry#*|}"
  cold=$(time_ms "$BASE_URL$path")
  samples=()
  for ((i = 0; i < N; i++)); do
    samples+=("$(time_ms "$BASE_URL$path")")
  done
  stats=$(printf "%s\n" "${samples[@]}" | sort -n | awk '
    { v[NR] = $1; sum += $1 }
    END {
      p50 = v[int((NR - 1) * 0.50) + 1]; p95 = v[int((NR - 1) * 0.95) + 1];
      printf "%.1f | %.1f | %.1f | %.1f", sum / NR, p50, p95, v[NR]
    }')
  echo "| $name | $cold | $stats |" >> "$OUT"
  echo "$name: cold ${cold}ms, avg|p50|p95|max = $stats"
done

echo
echo "Results written to $OUT"
