# Flyway Schema History Implementation Plan

**Goal:** Add Flyway schema history where `V1` represents the prod/main baseline and `V2` records the raw faculty division column.

**Architecture:** Spring Boot runs Flyway migrations from `classpath:db/migration` before JPA initialization. Existing non-empty DBs require manual `baselineVersion=1`; fresh DBs run `V1` then `V2`.

**Tech Stack:** Spring Boot 3.2.5, Flyway, PostgreSQL, H2 PostgreSQL mode for focused migration verification.

---

## Files

- Modify `build.gradle` to add Flyway.
- Modify `src/main/resources/application.yml` to keep Flyway enabled and manual-baseline-only.
- Modify `src/main/resources/application-dev.yml` to stop Hibernate schema mutation.
- Modify `src/test/resources/application.yml` to prevent Flyway from affecting unrelated tests.
- Create `src/main/resources/db/migration/V1__baseline_schema.sql`.
- Create `src/main/resources/db/migration/V2__add_raw_faculty_division_name_to_course_offerings.sql`.
- Create `src/test/java/com/chukchuk/haksa/global/db/FlywayMigrationTest.java`.

## Tasks

- [ ] Add Flyway dependency and baseline-safe Spring settings.
- [ ] Add `V1` baseline DDL without data rows and without `raw_faculty_division_name`.
- [ ] Add idempotent `V2` column migration and column comment.
- [ ] Add a focused fresh-DB migration test asserting `V1`, `V2`, and the final column.
- [ ] Run focused migration test and full test suite.
- [ ] Include numbered dev/prod baseline runbook in the PR body.

## Rollback

- Code rollback removes Flyway dependency/config and migration files before deployment.
- If prod migration starts after backup and fails during `V2`, restore from backup or repair Flyway only after confirming the partially applied DDL state.
