# Context: FEAT-176 API Test Suite Establishment

## 1. Feature Overview (Required)
- Purpose: Add executable API-focused tests that lock current behavior for every controller under `controller` packages and `application/api` package, including normal and exception paths.
- Scope:
  - In:
    - API integration tests for all current controllers in:
      - `src/main/java/**/controller/*Controller.java`
      - `src/main/java/**/application/api/*Controller.java`
    - API-related unit tests needed to isolate exception and branch behavior.
    - Validation of `SuccessResponse`/error response contract and representative error codes.
  - Out:
    - Business logic redesign/refactor.
    - Public API contract changes (URL, request/response schema, error code semantics).
    - New infrastructure dependencies.
- Expected Impact: Regression risk on login/portal sync/student/academic/graduation/auth API flows is reduced by executable tests that fail immediately when behavior changes.
- Stakeholder Confirmation: Request confirmed by stakeholder on 2026-02-22: "이번 브랜치에서는 테스트 코드를 작성... 통합 테스트와 단위 테스트... 예외 상황 검증... 모든 controller 패키지 하위 controller + application/api 하위 controller".

## 2. Domain Rules (Highest Priority, Required)
- Rule 1: Tests must verify existing behavior as-is; feature work must not change business logic.
- Rule 2: Every controller in scope must have at least one success-path integration test and at least one exception-path integration test (or explicit not-applicable note in test code comments when no controlled exception branch exists).
- Rule 3: Unit tests must focus on API-relevant branching/exception boundaries and must not duplicate integration assertions unnecessarily.
- Rule 4: For core API-related services touched in this branch (especially social sign-in and user merge/delete flows), unit tests must cover all identifiable public-method branches (success, not-found, existing/non-existing linkage, and representative failure branches).

- Mutable Rules:
  - The exact number of test cases per endpoint can grow as edge cases are discovered.
  - Test fixtures/mocks can be reorganized for readability and maintenance.
- Immutable Rules:
  - Scope includes all controllers currently under `controller` and `application/api` paths.
  - Existing API behavior and error semantics remain unchanged.

## 3. Use-case Scenarios (Required)

### Normal Flow
- Scenario Name: Controller endpoint returns expected success response
  - Trigger: Client calls an API endpoint with valid authentication/inputs.
  - Actor: API controller layer.
  - Steps:
    1. Request reaches target controller.
    2. Controller delegates to service/use-case.
    3. Endpoint returns expected HTTP status and success response wrapper/body.
  - Expected Result: Test asserts status, wrapper shape, and key response fields.

### Exception / Boundary Flow
- Scenario Name: Controller endpoint returns expected error for invalid/precondition-failed state
  - Condition: Typical exception conditions occur (e.g., already connected user, not connected user, session missing, token mismatch, entity not found).
  - Expected Behavior: Global exception handling returns expected HTTP status/error code/message contract, and tests verify representative cases.

## 4. Transaction and Consistency Policy (Required)
- Transaction Start Point: Per-request transaction boundaries in existing services remain unchanged.
- Transaction End Point: Existing service transaction completion/rollback behavior remains unchanged.
- Atomicity Scope: Tests must preserve current transactional semantics; no partial state persistence caused by tests.
- Eventual Consistency Allowed: Not introduced by this feature; tests should assume current synchronous API semantics.

## 5. API List (Optional / Required When Present)
- Endpoint:
  - Method: `POST`
  - Path: `/api/users/signin`
  - Authorization: Public
  - Idempotency: Non-idempotent token issuance
- Endpoint:
  - Method: `DELETE`
  - Path: `/api/users/delete`
  - Authorization: Authenticated
  - Idempotency: Effectively idempotent per deleted account state
- Endpoint:
  - Method: `POST`
  - Path: `/api/auth/refresh`
  - Authorization: Public (refresh token required)
  - Idempotency: Non-idempotent token reissue
- Endpoint:
  - Method: `POST`
  - Path: `/api/suwon-scrape/login`
  - Authorization: Authenticated
  - Idempotency: Last-write-wins credentials save
- Endpoint:
  - Method: `POST`
  - Path: `/api/suwon-scrape/start`
  - Authorization: Authenticated
  - Idempotency: Non-idempotent initialization/sync
- Endpoint:
  - Method: `POST`
  - Path: `/api/suwon-scrape/refresh`
  - Authorization: Authenticated
  - Idempotency: Non-idempotent refresh/sync
- Endpoint:
  - Method: `GET`
  - Path: `/api/academic/record`
  - Authorization: Authenticated
  - Idempotency: Idempotent read
- Endpoint:
  - Method: `GET`
  - Path: `/api/academic/summary`
  - Authorization: Authenticated
  - Idempotency: Idempotent read
- Endpoint:
  - Method: `GET`
  - Path: `/api/semester`
  - Authorization: Authenticated
  - Idempotency: Idempotent read
- Endpoint:
  - Method: `GET`
  - Path: `/api/semester/grades`
  - Authorization: Authenticated
  - Idempotency: Idempotent read
- Endpoint:
  - Method: `GET`
  - Path: `/api/graduation/progress`
  - Authorization: Authenticated
  - Idempotency: Idempotent read
- Endpoint:
  - Method: `POST`
  - Path: `/api/student/target-gpa`
  - Authorization: Authenticated
  - Idempotency: Non-idempotent update
- Endpoint:
  - Method: `GET`
  - Path: `/api/student/profile`
  - Authorization: Authenticated
  - Idempotency: Idempotent read
- Endpoint:
  - Method: `POST`
  - Path: `/api/student/reset`
  - Authorization: Authenticated
  - Idempotency: Non-idempotent reset

## 6. Exception Policy (Required)
- Error Code: `U03 USER_ALREADY_CONNECTED`
  - Condition: `/api/suwon-scrape/start` called by already connected user.
  - Message Convention: Existing `ErrorCode` message.
  - Handling Layer: Controller precheck + global exception handler.
  - User Exposure: Existing standardized error response.
- Error Code: `U04 USER_NOT_CONNECTED`
  - Condition: `/api/suwon-scrape/refresh` called by not-connected user.
  - Message Convention: Existing `ErrorCode` message.
  - Handling Layer: Controller precheck + global exception handler.
  - User Exposure: Existing standardized error response.
- Error Code: `A04 SESSION_EXPIRED`
  - Condition: Portal credentials not present before scrape fetch.
  - Message Convention: Existing `ErrorCode` message.
  - Handling Layer: Controller helper (`fetchPortalData`) + global exception handler.
  - User Exposure: Existing standardized error response.
- Error Code: Token/user/student/academic/graduation existing codes
  - Condition: Existing service exceptions during API execution.
  - Message Convention: Keep existing code/message/status mappings.
  - Handling Layer: Existing service + global exception handler path.
  - User Exposure: No schema change.

## 7. Phase Checklist
- [x] Phase 1 Context: requirements, domain rules, exception policy fixed
- [x] Phase 2 Domain: models, services, exceptions, pure tests written
- [ ] Phase 3 Application: orchestration, transactions, repository interface validation
- [ ] Phase 4 Infrastructure: persistence, external integration, technical implementation validated
- [ ] Phase 5 Global/Config: configuration, security, logging impact reviewed
- [x] Phase 6 API/Controller: endpoints, docs, validation flows confirmed

## 8. Generated File List (Required)
- Path: docs/context/20260222-feat-176-api-test-suite.md
- Description: Context specification for FEAT-176 API integration/unit test coverage scope and exception assertions.
- Layer: Context documentation
- Path: src/test/java/com/chukchuk/haksa/support/ApiControllerWebMvcTestSupport.java
- Description: Shared WebMvc controller-test support for auth context and common mocks.
- Layer: Test support
- Path: src/test/java/com/chukchuk/haksa/domain/user/controller/UserControllerApiIntegrationTest.java
- Description: API integration tests for `/api/users/signin` and `/api/users/delete` success/exception paths.
- Layer: API integration test
- Path: src/test/java/com/chukchuk/haksa/domain/auth/controller/AuthControllerApiIntegrationTest.java
- Description: API integration tests for `/api/auth/refresh` success/exception paths.
- Layer: API integration test
- Path: src/test/java/com/chukchuk/haksa/application/api/SuwonScrapeControllerApiIntegrationTest.java
- Description: API integration tests for portal login/start/refresh precondition and exception branches.
- Layer: API integration test
- Path: src/test/java/com/chukchuk/haksa/domain/student/controller/StudentControllerApiIntegrationTest.java
- Description: API integration tests for student profile and target GPA validation error handling.
- Layer: API integration test
- Path: src/test/java/com/chukchuk/haksa/domain/academic/record/controller/AcademicRecordControllerApiIntegrationTest.java
- Description: API integration tests for academic record and summary error path.
- Layer: API integration test
- Path: src/test/java/com/chukchuk/haksa/domain/academic/record/controller/SemesterControllerApiIntegrationTest.java
- Description: API integration tests for semester list/grades and empty-data error path.
- Layer: API integration test
- Path: src/test/java/com/chukchuk/haksa/domain/graduation/controller/GraduationControllerApiIntegrationTest.java
- Description: API integration tests for graduation progress success and missing-requirement exception path.
- Layer: API integration test
- Path: src/test/java/com/chukchuk/haksa/domain/user/service/UserServiceUnitTests.java
- Description: Unit tests for user merge/deletion/sign-in token issuance behaviors.
- Layer: Domain unit test
