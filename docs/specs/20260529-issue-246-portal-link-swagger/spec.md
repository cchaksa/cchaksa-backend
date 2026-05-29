# 20260529-issue-246-portal-link-swagger Spec

## 1. Feature Overview
- Purpose: `origin/dev` 기준 API의 실제 응답 구조, 상태 코드, 인증 요구사항과 Swagger 응답 schema/example을 일치시킨다.
- Scope.
  - In: 포털 링크 API 3개, 내부 스크래핑 콜백, 인증이 필요한 API의 공통 401 응답, health/sentry-test Swagger schema.
  - Out: 실제 런타임 응답 body 변경, 포털 연동 비즈니스 로직 변경, DB/schema 변경.
- Expected Impact: Swagger UI의 Example Value와 응답 코드가 실제 배포 응답처럼 표시된다.
- Stakeholder Confirmation: 사용자 요청 “그러면 이것도 feat/246에서 이 이슈 해결하고, dev로 push 한 후 재배포도 진행해.”를 추가 구현 및 배포 승인으로 기록한다.

## 2. Domain Rules
- Rule 1: 실제 API 응답 구조는 기존 `SuccessResponse<T>`를 유지한다.
- Rule 2: Swagger 문서의 성공 응답 schema는 실제 최상위 JSON 구조와 일치해야 한다.
- Rule 3: 내부 DTO인 `AcceptedResponse`, `JobStatusResponse`, `JobSummaryResponse`의 필드명은 변경하지 않는다.
- Rule 4: 인증이 필요한 API는 공통 인증 실패 401 응답을 문서화한다.
- Rule 5: 공개 API는 Swagger에서 bearerAuth를 요구하지 않아야 한다.
- Mutable Rules: Swagger wrapper class와 controller docs annotation은 변경 가능하다.
- Immutable Rules: 포털 링크 job 생성/조회의 상태 전이, 서비스 로직, 엔드포인트 path는 변경하지 않는다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 포털 링크 job 생성 Swagger 확인.
  - Trigger: Swagger UI에서 `/portal/link` 202 Example Value를 확인한다.
  - Actor: 프론트엔드 개발자.
  - Steps: API 문서를 열고 202 성공 응답 schema/example을 본다.
  - Expected Result: 최상위에 `success`, `data`, `message`가 있고 `data` 안에 `job_id`, `polling_endpoint`, `status`가 있다.

### Exception / Boundary Flow
- Scenario Name: 실제 응답 body 보존.
  - Condition: 컨트롤러 테스트가 기존 성공 응답 body를 검증한다.
  - Expected Behavior: 런타임 JSON 필드는 기존과 동일하다.
- Scenario Name: 보호 API 무인증 요청 문서화.
  - Condition: bearer token 없이 보호 API를 호출한다.
  - Expected Behavior: 실제 공통 401 error wrapper가 Swagger 응답 코드에 포함된다.
- Scenario Name: 내부 콜백 서명 실패 문서화.
  - Condition: 잘못된 HMAC 서명으로 `/internal/scrape-results`를 호출한다.
  - Expected Behavior: 실제 401 `ErrorResponseWrapper`가 Swagger에 표시된다.

## 4. Transaction / Consistency
- Transaction Start Point: 없음.
- Transaction End Point: 없음.
- Atomicity Scope: 문서 schema 변경만 수행한다.
- Eventual Consistency Allowed: 해당 없음.

## 5. API List
- Endpoint: `/portal/link`.
  - Method: POST.
  - Request DTO: `PortalLinkDto.LinkRequest`.
  - Response DTO: `SuccessResponse<PortalLinkDto.AcceptedResponse>`.
  - Authorization: Bearer.
  - Idempotency: `Idempotency-Key`.
- Endpoint: `/portal/link/jobs/{jobId}`.
  - Method: GET.
  - Response DTO: `SuccessResponse<PortalLinkDto.JobStatusResponse>`.
- Endpoint: `/portal/link/jobs/{jobId}/summary`.
  - Method: GET.
  - Response DTO: `SuccessResponse<PortalLinkDto.JobSummaryResponse>`.
- Endpoint: `/internal/scrape-results`.
  - Method: POST.
  - Response DTO: `SuccessResponse<MessageOnlyResponse>`.
  - Authorization: HMAC signature headers.
- Endpoint: 보호 API 전체.
  - Method: GET/POST/DELETE.
  - Error Response DTO: `ErrorResponseWrapper` for 401.

## 6. Exception Policy
- Error Code: 기존 ErrorResponseWrapper 유지.
  - Condition: 입력 오류, 인증 오류, 중복 요청, job 없음, 내부 콜백 서명 실패.
  - Message Convention: 기존 예외 처리 정책 유지.
  - Handling Layer: 기존 GlobalExceptionHandler 유지.
  - User Exposure: Swagger schema media type만 JSON으로 명시한다.

## 7. Phase Checklist
- [x] Phase 1 Spec fixed.
- [ ] Phase 6 API/Controller documentation fixed.
- [ ] Tests pass.

## 8. Generated File List
- Path: `src/test/java/com/chukchuk/haksa/domain/portal/controller/PortalLinkOpenApiTest.java`.
  - Description: 포털 링크 Swagger 성공 응답 wrapper schema 검증.
  - Layer: API test.
- Path: `src/main/java/com/chukchuk/haksa/domain/portal/wrapper/*.java`.
  - Description: 포털 링크 성공 응답 문서용 wrapper.
  - Layer: API documentation.
- Path: `src/main/java/com/chukchuk/haksa/global/config/OpenApiConfig.java`.
  - Description: OpenAPI 공통 security/media type/401 응답 정합성 보정.
  - Layer: Global documentation config.
