# Issue 290 Context Notes

- Work starts from `dev`, which matched `origin/dev` before branch creation.
- Existing unrelated dirty files were present before this work and must not be staged.
- This issue is intentionally limited to dead code and commented Redis leftovers.
- OpenAPI source-of-truth, WebFlux removal, portal mapping deduplication, and domain model shrink are deferred to #291.
- Reference checks showed the deleted candidates were either fully commented out or referenced only by their own files.
- Redis is no longer advertised as a selectable cache or credential-store option in the default application config.
- Post-delete `rg` checks found no remaining references to the removed class names or Redis-only config keys under `src/main`, `src/test`, or `build.gradle`.
- `./gradlew test` passed after the cleanup.
