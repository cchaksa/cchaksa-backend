# Issue 291 Plan

## Goal

Reduce structural cost left after #290 while preserving public API and runtime behavior.

## Scope

- Prefer deletion or small local helpers over new abstractions.
- Keep API contracts, DB schema, auth behavior, and transaction boundaries unchanged.
- Treat OpenAPI source-of-truth changes as optional unless the current code makes a small safe cleanup obvious.
- Remove `spring-webflux` only if `PortalClient` can keep the same blocking behavior with existing Spring MVC tooling.
- Deduplicate portal initialization and refresh mapping only after reading both flows.
- Remove domain model helpers only when reference checks prove they are unused.

## Verification

- Focused `rg` checks for removed methods, classes, and dependencies.
- `./gradlew test`.
