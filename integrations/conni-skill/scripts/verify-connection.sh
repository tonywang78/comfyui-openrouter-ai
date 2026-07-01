#!/usr/bin/env bash
# Verify Conni-X-Pro API connectivity with CONNI_API_KEY.
set -euo pipefail

BASE_URL="${CONNI_API_BASE_URL:-http://localhost:9898/api}"
API_KEY="${CONNI_API_KEY:-}"

if [[ -z "$API_KEY" ]]; then
  echo "ERROR: CONNI_API_KEY is not set" >&2
  exit 1
fi

echo "Base URL: $BASE_URL"
echo "Checking health..."
health=$(curl -sf -H "X-Api-Key: $API_KEY" "$BASE_URL/client/health")
echo "  health: $health"

echo "Checking credits..."
credits=$(curl -sf -H "X-Api-Key: $API_KEY" "$BASE_URL/user/get/credits")
echo "  credits: $credits"

echo "OK: connection verified"
