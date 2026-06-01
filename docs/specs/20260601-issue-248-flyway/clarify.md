# Clarifications

| Date | Decision | Rationale |
| --- | --- | --- |
| 2026-06-01 | Existing dev/prod DBs are not migrated by application startup until baseline is manually recorded. | Prevents accidental baseline creation on a wrong DB and keeps operational ownership explicit. |
| 2026-06-01 | `V1` excludes `raw_faculty_division_name`; `V2` adds it with `IF NOT EXISTS`. | Keeps the dev-only schema delta visible in migration history while allowing dev's existing column to no-op. |
| 2026-06-01 | Test profile disables auto Flyway and uses an explicit focused migration test. | Existing tests rely on Hibernate `create-drop`; the focused test verifies the SQL path without changing every test fixture. |
