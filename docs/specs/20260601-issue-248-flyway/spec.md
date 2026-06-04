# Issue 248: Flyway Schema History

## Purpose

Introduce Flyway as the DB schema history manager without hiding the existing dev/prod schema difference in the baseline.

## Decisions

- `V1` is the current prod/main schema baseline and contains no data rows.
- `V2` records the `course_offerings.raw_faculty_division_name` column added after the prod/main baseline.
- Existing dev/prod databases must be baselined manually before migration is allowed.
- Lambda runtime profiles (`dev`, `prod`) disable Spring Boot Flyway auto-run; migrations run only as a separate pre-deploy operation.
- `baseline-on-migrate` stays disabled so an unprepared non-empty DB fails fast in explicit migration jobs instead of silently creating a baseline.
- Test profile disables Spring Boot Flyway auto-run; migration SQL is verified by a focused Flyway test.

## Scope

- Add Flyway dependency and Spring configuration.
- Disable Flyway auto-run in AWS Lambda runtime profiles.
- Add `src/main/resources/db/migration/V1__baseline_schema.sql`.
- Add `src/main/resources/db/migration/V2__add_raw_faculty_division_name_to_course_offerings.sql`.
- Add focused migration verification for a fresh DB path.
- Document the existing dev/prod one-time baseline runbook for PR reviewers and operators.

## Out Of Scope

- Running baseline or migrate against live dev/prod DBs from this Codex session.
- Moving seed/reference data into migrations.
- Changing public API behavior.

## External Operations

Existing dev/prod DBs require manual first-time operations because they already contain schema and data.

1. Dev DB
   - Run `flyway baseline -baselineVersion=1` against the dev DB from a pre-deploy migration job, not from the API Lambda runtime.
   - Run `flyway migrate` against the dev DB from the same migration job.
   - Confirm `flyway_schema_history` contains baseline version `1` and successful version `2`.
   - Confirm `course_offerings.raw_faculty_division_name` exists; `V2` is idempotent if the column already exists.

2. Prod DB
   - Take and verify a DB backup before Flyway changes.
   - Run `flyway baseline -baselineVersion=1` against the prod DB from a pre-deploy migration job, not from the API Lambda runtime.
   - Run `flyway migrate` against the prod DB from the same migration job.
   - Confirm `flyway_schema_history` contains baseline version `1` and successful version `2`.
   - Confirm `course_offerings.raw_faculty_division_name` exists.

## Verification

- `./gradlew test --tests com.chukchuk.haksa.global.db.FlywayMigrationTest`
- `./gradlew test`
