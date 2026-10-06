#!/usr/bin/env bash

set -euo pipefail

base_url="${1:-https://dev.admin.cchaksa.com}"

if [ "${base_url}" != "https://dev.admin.cchaksa.com" ]; then
  echo "Development verification requires https://dev.admin.cchaksa.com." >&2
  exit 1
fi

verification_dir="$(mktemp -d)"
trap 'rm -rf "${verification_dir}"' EXIT

csrf_headers="${verification_dir}/csrf.headers"
csrf_body="${verification_dir}/csrf.body"

csrf_status=$(curl --silent --show-error \
  --output "${csrf_body}" \
  --dump-header "${csrf_headers}" \
  --write-out '%{http_code}' \
  "${base_url}/api/admin/auth/csrf")

csrf_content_type=$(awk 'tolower($0) ~ /^content-type:/ { value=$0 } END { sub(/^[^:]+:[[:space:]]*/, "", value); sub(/\r$/, "", value); print value }' "${csrf_headers}")
csrf_cache_control=$(awk 'tolower($0) ~ /^cache-control:/ { value=$0 } END { sub(/^[^:]+:[[:space:]]*/, "", value); sub(/\r$/, "", value); print tolower(value) }' "${csrf_headers}")

if [ "${csrf_status}" != "204" ]; then
  echo "Admin CSRF readiness verification failed: expected 204, got ${csrf_status}" >&2
  exit 1
fi

if [[ "${csrf_content_type}" == text/html* ]] || grep -qi '<!doctype html\|<html' "${csrf_body}"; then
  echo "Admin CSRF readiness verification failed: response was replaced by SPA HTML." >&2
  exit 1
fi

if [[ "${csrf_cache_control}" != *no-store* ]]; then
  echo "Admin CSRF readiness verification failed: Cache-Control must include no-store." >&2
  exit 1
fi

"$(dirname "$0")/verify_admin_web_production.sh" "${base_url}"
echo "Admin web development readiness verification passed."
