#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: ./scripts/new-spec.sh <YYYYMMDD-slug> [--lite]

Creates docs/specs/<YYYYMMDD-slug>/ with Spec Kit hybrid templates:
  Standard (default): spec.md, clarify.md, plan.md, tasks.md
  Lite (--lite): spec-lite.md only
USAGE
}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SPECS_DIR="$ROOT_DIR/docs/specs"

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

TARGET_SLUG="$1"
shift || true

MODE="standard"
if [[ $# -gt 0 ]]; then
  if [[ "$1" == "--lite" ]]; then
    MODE="lite"
  else
    echo "Unknown option: $1" >&2
    usage
    exit 1
  fi
fi

if [[ ! "$TARGET_SLUG" =~ ^[0-9]{8}-[a-z0-9-]+$ ]]; then
  echo "Slug must follow YYYYMMDD-something (got: $TARGET_SLUG)" >&2
  exit 1
fi

DEST="$SPECS_DIR/$TARGET_SLUG"
if [[ -e "$DEST" ]]; then
  echo "Destination already exists: $DEST" >&2
  exit 1
fi

mkdir -p "$DEST"

create_file() {
  local dest_name="$2"
  local title="$1"
  printf '# %s\n\n' "$title" > "$DEST/$dest_name"
}

if [[ "$MODE" == "lite" ]]; then
  create_file "Spec Lite" "spec-lite.md"
  echo "Lite spec created at $DEST/spec-lite.md"
else
  create_file "Spec" "spec.md"
  create_file "Clarify" "clarify.md"
  create_file "Plan" "plan.md"
  create_file "Tasks" "tasks.md"
  echo "Standard spec bundle created under $DEST"
fi

echo "Next steps:"
echo "  1. Fill in the template(s)."
echo "  2. Link the directory to the corresponding branch/Context."
