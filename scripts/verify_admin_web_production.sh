#!/usr/bin/env bash

set -euo pipefail

base_url="${1:-https://admin.cchaksa.com}"
verification_dir="$(mktemp -d)"
trap 'rm -rf "${verification_dir}"' EXIT

spa_headers="${verification_dir}/spa.headers"
spa_body="${verification_dir}/spa.body"
api_headers="${verification_dir}/api.headers"
api_body="${verification_dir}/api.body"

spa_status=$(curl --silent --show-error \
  --output "${spa_body}" \
  --dump-header "${spa_headers}" \
  --write-out '%{http_code}' \
  "${base_url}/__routing_verification__")

spa_content_type=$(awk 'tolower($0) ~ /^content-type:/ { value=$0 } END { sub(/^[^:]+:[[:space:]]*/, "", value); sub(/\r$/, "", value); print value }' "${spa_headers}")

if [ "${spa_status}" != "200" ] || [[ "${spa_content_type}" != text/html* ]]; then
  echo "SPA fallback verification failed: status=${spa_status}, content-type=${spa_content_type}" >&2
  exit 1
fi

api_status=$(curl --silent --show-error \
  --output "${api_body}" \
  --dump-header "${api_headers}" \
  --write-out '%{http_code}' \
  "${base_url}/api/admin/auth/me")

api_content_type=$(awk 'tolower($0) ~ /^content-type:/ { value=$0 } END { sub(/^[^:]+:[[:space:]]*/, "", value); sub(/\r$/, "", value); print value }' "${api_headers}")

if [ "${api_status}" != "401" ] && [ "${api_status}" != "403" ]; then
  echo "Admin API verification failed: expected 401 or 403, got ${api_status}" >&2
  exit 1
fi

if [[ "${api_content_type}" == text/html* ]] || grep -qi '<!doctype html\|<html' "${api_body}"; then
  echo "Admin API verification failed: API error was replaced by SPA HTML" >&2
  exit 1
fi

echo "Admin web routing verification passed."
