# Context: Sentry MDC Tag Normalization

## 1. Feature Overview (Required)
- Purpose: Guarantee that the MDC values populated during graduation requirement failures (student_code, admission_year, department ids, major_type) surface as first-class Sentry tags so incidents can be filtered by those attributes instead of being buried in Context payloads, and expand the observable metadata with authenticated user identifiers (userId) for cross-request debugging.
- Scope:
  - In:
    - Global exception handling that currently copies MDC values into Sentry scope (e.g., `GlobalExceptionHandler#handleBase`).
    - Graduation domain services that stuff MDC keys before raising `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND`.
    - Observability configuration impacting MDC propagation (Logback/Sentry integration, MDC lifecycle management in filters).
    - MDC population filters that enrich requests with user identity (`MdcUserEnricherFilter`).
  - Out:
    - Any change to HTTP APIs, DTOs, or persistence logic retrieving graduation data.
    - Sentry project/organization configuration outside of application code.
    - Addition of new MDC keys beyond the existing graduation metadata set unless explicitly asked later.
- Expected Impact: Sentry issues become filterable by student/graduation metadata as well as authenticated userId, which reduces triage time, makes dashboards accurate, and avoids manual searching through JSON contexts.
- Stakeholder Confirmation: Requirement provided by requester on 2026-02-11 ("MDC 값을 Tag로 설정하여 검색이 용이하도록 변경").

## 2. Domain Rules (Highest Priority, Required)
- Rule 1: When graduation requirement data is missing, the domain service must populate MDC keys `student_code`, `admission_year`, `primary_department_id`, `secondary_department_id`, and `major_type` before propagating the exception.
- Rule 2: Any Sentry capture triggered for `BaseException` or graduation-specific error codes must elevate the above MDC keys, plus `userId` when present, into Sentry tags atomically so that tag filters always reflect the MDC snapshot.
- Rule 3: Adding or removing MDC entries must never leak PII beyond the approved fields nor alter the business semantics of graduation flows; tags are purely observational metadata.

- Mutable Rules:
  - Additional MDC → Tag mappings (including user identity variants) can be appended in the future as new observability dimensions emerge, provided stakeholders approve them.
  - The technical mechanism for extracting MDC values (manual scope mutation vs. Logback appender enrichment) may evolve without changing observable behavior.
- Immutable Rules:
  - The set of graduation MDC keys listed in Rule 1 must remain consistent and populated from the authoritative student entities when applicable.
  - Sentry tag propagation must not change request outcomes or generate additional Sentry events; it only enriches metadata for existing captures.

## 3. Use-case Scenarios (Required)

### Normal Flow
- Scenario Name: Graduation requirement lookup fails with traceable metadata
  - Trigger: `GraduationService` detects missing graduation requirement data and throws `CommonException` with code `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND` after putting graduation metadata into MDC.
  - Actor: GraduationService
  - Steps:
    1. Service populates MDC keys with student code, admission year, major ids, and major type.
    2. Exception propagates to `GlobalExceptionHandler#handleBase`.
    3. Handler copies each MDC value into corresponding Sentry tag and sets fingerprints/levels.
    4. Sentry event includes tags so observability teams can filter incidents by student/department attributes.
  - Expected Result: Sentry tag panel lists every MDC key/value pair, enabling saved searches by student_code, admission_year, etc., without altering HTTP response semantics.

### Exception / Boundary Flow
- Scenario Name: MDC key missing or blank
  - Condition: Graduation metadata is absent (e.g., anonymous user) or MDC cleanup already occurred before exception capture.
  - Expected Behavior: Handler skips only the missing keys while still tagging the remaining ones; lack of MDC data must not break Sentry capture nor throw additional errors.

## 4. Transaction and Consistency Policy (Required)
- Transaction Start Point: Not applicable; tagging occurs after domain transactions finish and during exception handling.
- Transaction End Point: Not applicable; MDC to Tag promotion happens outside persistence transactions.
- Atomicity Scope: For each exception capture, either all available MDC keys are converted to tags or none if Sentry scope is unavailable; the promotion must not partially modify system state.
- Eventual Consistency Allowed: Yes; tagging is observational and may be skipped when MDC is empty without affecting business data.

## 5. API List (Optional / Required When Present)
- Not applicable; no external API/request DTO/response DTO changes are part of this context.

## 6. Exception Policy (Required)
- Error Code: `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND`
  - Condition: Missing graduation rule data for a student's (primary/secondary) major combination.
  - Message Convention: Reuse existing `ErrorCode` message; no new wording required.
  - Handling Layer: `GlobalExceptionHandler#handleBase` ensures tags are set before returning 4xx response.
  - User Exposure: Client receives unchanged error payload; additional metadata only appears in Sentry.
- Error Code: Generic `BaseException` codes
  - Condition: Any business exception carrying graduation MDC values.
  - Message Convention: Existing `BaseException` messages.
  - Handling Layer: Same handler path with MDC → tag elevation.
  - User Exposure: None beyond existing error response.

## 7. Phase Checklist
- [x] Phase 1 Context: requirements, domain rules, exception policy fixed
- [ ] Phase 2 Domain: models, services, exceptions, pure tests written
- [ ] Phase 3 Application: orchestration, transactions, repository interface validation
- [ ] Phase 4 Infrastructure: persistence, external integration, technical implementation validated
- [ ] Phase 5 Global/Config: configuration, security, logging impact reviewed
- [ ] Phase 6 API/Controller: endpoints, docs, validation flows confirmed

## 8. Generated File List (Required)
- Path: docs/context/20260211-sentry-mdc-tag.md
  - Description: Context specification for promoting MDC graduation metadata to Sentry tags.
  - Layer: Context documentation
