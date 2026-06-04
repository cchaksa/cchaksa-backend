# Plan

## Implementation

1. Add failing tests for the duration query response and server-side terminal timestamp behavior.
2. Add Flyway `V4__add_portal_link_duration_columns.sql` with a `created_at` backfill for existing rows.
3. Extend `ScrapeJob` with `linkStartedAt`, `linkEndedAt`, and terminal transition methods that record server end time.
4. Pass `requestedAt` into new job creation so `linkStartedAt` uses the request acceptance timestamp.
5. Add `PortalLinkDto.JobDurationResponse` and `PortalLinkJobQueryService.getJobDuration`.
6. Add `GET /portal/link/jobs/{jobId}/duration` controller/docs/wrapper support.
7. Update OpenAPI contract coverage and static `public/openapi.yaml`.

## Verification

- Focused application tests:
  - `PortalLinkJobQueryService` duration tests
  - `PortalJobQueryControllerApiIntegrationTest`
  - `ScrapeResultCallbackServiceUnitTests` or transaction service tests for terminal server timestamp
- OpenAPI tests:
  - `PortalLinkOpenApiTest`
  - `OpenApiResponseContractTest` when static/openapi wrapper changes affect global contracts
- Broader check:
  - `./gradlew test` because this changes public API, persistence, and callback behavior.
