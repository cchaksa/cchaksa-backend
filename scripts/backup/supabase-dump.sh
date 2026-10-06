#!/usr/bin/env bash
set -euo pipefail

required_variables=(
  BACKUP_ENCRYPTION_PASSPHRASE
  BACKUP_S3_BUCKET
  SUPABASE_DB_URL
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required environment variable: ${variable_name}." >&2
    exit 1
  fi
done

backup_prefix="${BACKUP_S3_PREFIX:-supabase-db}"
runner_temp_dir="${RUNNER_TEMP:-/tmp}"
backup_dir="$(mktemp -d "${runner_temp_dir}/supabase-backup.XXXXXX")"
passphrase_file="${backup_dir}/passphrase"

cleanup() {
  rm -rf "${backup_dir}"
}
trap cleanup EXIT

umask 077
printf '%s' "${BACKUP_ENCRYPTION_PASSPHRASE}" > "${passphrase_file}"

require_nonempty_file() {
  local file_path="$1"

  if [[ ! -s "${file_path}" ]]; then
    echo "Backup output is missing or empty: ${file_path##*/}." >&2
    exit 1
  fi
}

encrypt_dump() {
  local source_path="$1"
  local encrypted_path="${source_path}.gz.gpg"

  gzip --no-name --stdout "${source_path}" | \
    gpg --batch --yes --no-tty --pinentry-mode loopback \
      --passphrase-file "${passphrase_file}" \
      --symmetric --cipher-algo AES256 \
      --output "${encrypted_path}"

  require_nonempty_file "${encrypted_path}"
  printf '%s\n' "${encrypted_path}"
}

verify_s3_object() {
  local s3_key="$1"
  local content_length

  content_length="$(aws s3api head-object \
    --bucket "${BACKUP_S3_BUCKET}" \
    --key "${s3_key}" \
    --query 'ContentLength' \
    --output text)"

  if [[ ! "${content_length}" =~ ^[1-9][0-9]*$ ]]; then
    echo "Uploaded backup is missing or empty: ${s3_key}." >&2
    exit 1
  fi
}

upload_copy() {
  local source_path="$1"
  local s3_key="$2"

  aws s3 cp --only-show-errors "${source_path}" "s3://${BACKUP_S3_BUCKET}/${s3_key}"
  verify_s3_object "${s3_key}"
}

daily_timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
daily_prefix="${backup_prefix}/daily/$(date -u +%Y/%m/%d)/${daily_timestamp}"
kst_day="$(TZ=Asia/Seoul date +%d)"
kst_month="$(TZ=Asia/Seoul date +%Y/%m)"

supabase db dump --db-url "${SUPABASE_DB_URL}" --role-only -f "${backup_dir}/roles.sql"
supabase db dump --db-url "${SUPABASE_DB_URL}" -f "${backup_dir}/schema.sql"
supabase db dump --db-url "${SUPABASE_DB_URL}" --data-only --use-copy \
  -x "storage.buckets_vectors" \
  -x "storage.vector_indexes" \
  -f "${backup_dir}/data.sql"

dump_names=(roles schema data)
encrypted_files=()

for dump_name in "${dump_names[@]}"; do
  source_file="${backup_dir}/${dump_name}.sql"
  require_nonempty_file "${source_file}"
  encrypted_files+=("$(encrypt_dump "${source_file}")")
done

for encrypted_file in "${encrypted_files[@]}"; do
  object_name="$(basename "${encrypted_file}")"
  upload_copy "${encrypted_file}" "${daily_prefix}/${object_name}"
  upload_copy "${encrypted_file}" "${backup_prefix}/latest/${object_name}"

  if [[ "${kst_day}" == "01" ]]; then
    upload_copy "${encrypted_file}" "${backup_prefix}/monthly/${kst_month}/${daily_timestamp}/${object_name}"
  fi
done

{
  echo "## Database Backup"
  echo
  printf '%s\n' "- Daily backup: \`${daily_prefix}\`."
  printf '%s\n' "- Latest copy updated: \`${backup_prefix}/latest/\`."
  if [[ "${kst_day}" == "01" ]]; then
    printf '%s\n' "- Monthly backup created: \`${backup_prefix}/monthly/${kst_month}/${daily_timestamp}\`."
  fi
  echo "- Files verified: ${#encrypted_files[@]}."
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
