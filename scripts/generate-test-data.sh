#!/usr/bin/env bash
# Inserts N synthetic products (on top of the 114 seed products) for benchmarking.
#
#   scripts/generate-test-data.sh 10000            # against the Docker stack (default)
#   scripts/generate-test-data.sh 50000 --local    # using backend/target/quickfind-backend.jar and .env
#
# Run it several times to grow the catalog: 10k -> 50k -> 100k (generated rows add up).
# Restart the backend afterwards so the autocomplete Trie and price index include the new rows.
set -euo pipefail

COUNT="${1:-10000}"
MODE="${2:---docker}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! [[ "$COUNT" =~ ^[0-9]+$ ]] || [ "$COUNT" -lt 1 ]; then
  echo "Usage: $0 <count> [--docker|--local]" >&2
  exit 1
fi

ARGS=(--spring.profiles.active=generate "--quickfind.generate.count=$COUNT")

if [ "$MODE" = "--local" ]; then
  JAR="backend/target/quickfind-backend.jar"
  [ -f "$JAR" ] || (cd backend && ./mvnw -q -DskipTests package)
  java -jar "$JAR" "${ARGS[@]}"
else
  docker compose up -d mysql redis
  docker compose run --rm --no-deps backend "${ARGS[@]}"
  echo "Restarting the backend so it rebuilds its in-memory indexes..."
  docker compose restart backend
fi
