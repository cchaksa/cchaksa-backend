# Tasks

## Checklist

- [x] Add failing tests for duration response states.
- [x] Add failing tests for server-side terminal timestamp recording.
- [x] Add Flyway migration for `link_started_at` and `link_ended_at`.
- [x] Extend `ScrapeJob` duration fields and transition methods.
- [x] Add duration query service/controller/DTO.
- [x] Update OpenAPI docs and static spec.
- [x] Run focused tests.
- [x] Run `./gradlew test`.

## Verification Log

| # | Command | Result | Date |
|---|---|---|---|
| 1 | `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalLinkJobQueryServiceUnitTests --tests com.chukchuk.haksa.domain.portal.controller.PortalJobQueryControllerApiIntegrationTest --tests com.chukchuk.haksa.application.portal.PortalLinkJobTxServiceUnitTests --rerun-tasks --console=plain` | Failed as expected. Missing duration DTO/service/entity APIs. | 2026-06-04 |
| 2 | `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalLinkJobQueryServiceUnitTests --tests com.chukchuk.haksa.domain.portal.controller.PortalJobQueryControllerApiIntegrationTest --tests com.chukchuk.haksa.application.portal.PortalLinkJobTxServiceUnitTests --rerun-tasks --console=plain` | Passed. | 2026-06-04 |
| 3 | `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalLinkJobQueryServiceUnitTests --tests com.chukchuk.haksa.domain.portal.controller.PortalJobQueryControllerApiIntegrationTest --tests com.chukchuk.haksa.application.portal.PortalLinkJobTxServiceUnitTests --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest --tests com.chukchuk.haksa.global.config.OpenApiResponseContractTest --rerun-tasks --console=plain` | Passed. | 2026-06-04 |
| 4 | `./gradlew test --tests com.chukchuk.haksa.application.portal.ScrapeResultCallbackServiceUnitTests --rerun-tasks --console=plain` | Passed. | 2026-06-04 |
| 5 | `./gradlew test --console=plain` | Failed. `FlywayMigrationTest` still expected migrations V1-V3 only. | 2026-06-04 |
| 6 | `./gradlew test --tests com.chukchuk.haksa.global.db.FlywayMigrationTest --rerun-tasks --console=plain` | Failed. H2 did not accept multi-column `ALTER TABLE ... ADD COLUMN`. | 2026-06-04 |
| 7 | `./gradlew test --tests com.chukchuk.haksa.global.db.FlywayMigrationTest --rerun-tasks --console=plain` | Passed. | 2026-06-04 |
| 8 | `./gradlew test --console=plain` | Passed. | 2026-06-04 |
| 9 | `./gradlew test --console=plain` | Passed after migration backfill edge-case hardening. | 2026-06-04 |
