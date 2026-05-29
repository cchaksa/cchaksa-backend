# 20260529-issue-246-portal-link-swagger Spec

## 1. Feature Overview
- Purpose: `origin/dev` 기준 포털 링크 API의 실제 `SuccessResponse` 응답 구조와 Swagger 성공 응답 schema/example을 일치시킨다.
- Scope.
  - In: `/portal/link`, `/portal/link/jobs/{jobId}`, `/portal/link/jobs/{jobId}/summary` 성공 응답 Swagger schema.
  - Out: 실제 런타임 응답 body 변경, 포털 연동 비즈니스 로직 변경, DB/schema 변경.
- Expected Impact: Swagger UI의 Example Value가 실제 응답처럼 `success`, `data`, `message` 래퍼를 포함한다.
- Stakeholder Confirmation: 사용자 요청 “오케이 문제인 부분 모두 해결하자 feat/246 만들어서 해결해.”를 구현 승인으로 기록한다.

## 2. Domain Rules
- Rule 1: 실제 API 응답 구조는 기존 `SuccessResponse<T>`를 유지한다.
- Rule 2: Swagger 문서의 성공 응답 schema는 실제 최상위 JSON 구조와 일치해야 한다.
- Rule 3: 내부 DTO인 `AcceptedResponse`, `JobStatusResponse`, `JobSummaryResponse`의 필드명은 변경하지 않는다.
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

## 6. Exception Policy
- Error Code: 기존 ErrorResponseWrapper 유지.
  - Condition: 입력 오류, 인증 오류, 중복 요청, job 없음.
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
