# Issue 290 Plan

## Goal

Remove dead code and commented Redis leftovers without changing runtime behavior.

## Scope

- Re-check references for each deletion candidate before editing.
- Delete only unused classes or commented-out implementations.
- Keep public APIs, DB schema, auth behavior, and portal behavior unchanged.
- Leave larger structural cleanup to #291.

## Verification

- `./gradlew test`.
- `rg` checks for removed class names under `src/main` and `src/test`.
