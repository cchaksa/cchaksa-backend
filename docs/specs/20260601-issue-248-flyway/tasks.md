# Tasks

- [x] Add Flyway dependency/configuration
- [x] Add `V1__baseline_schema.sql`
- [x] Add `V2__add_raw_faculty_division_name_to_course_offerings.sql`
- [x] Add focused Flyway migration test
- [x] Run focused and full verification
- [x] Open PR with numbered dev/prod baseline runbook

## Verification Results

- `./gradlew test --tests com.chukchuk.haksa.global.db.FlywayMigrationTest` passed on 2026-06-01.
- `./gradlew test` passed on 2026-06-01.
- Draft PR: https://github.com/cchaksa/cchaksa-backend/pull/249
