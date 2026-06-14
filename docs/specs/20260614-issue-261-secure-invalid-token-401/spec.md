# Spec

## 1. Feature Overview
- Purpose: secure endpoint에서 유효하지 않은 access token 요청이 502로 새지 않고 401 인증 실패 응답으로 정리되도록 수정한다.
- Scope
  - In:
    - `JwtAuthenticationFilter` 인증 실패 처리.
    - 만료, 형식/서명 invalid, 탈퇴/삭제 사용자 토큰의 401 응답 회귀 테스트.
    - 영향 endpoint: `GET /api/users/me`, `GET /api/student/profile` 포함 모든 secure endpoint.
  - Out:
    - 로그인, refresh token 정책 변경.
    - 공개 API 응답 계약 변경.
    - DB 스키마, 트랜잭션, 외부 API 변경.
- Expected Impact:
  - 클라이언트가 invalid access token을 받았을 때 502 서버 장애로 오인하지 않고 401 인증 실패로 처리할 수 있다.
  - Gateway/ALB upstream 오류 의미를 갖는 502를 인증 실패 케이스에서 제거한다.
- Stakeholder Confirmation:
  - 2026-06-14 사용자 확인: GitHub issue 번호는 261이며 "바로 작업 시작해"라고 지시했다.
  - 2026-06-14 사용자 확인: invalid token 케이스는 401로 던지는 것이 더 맞는지 확인했고, 401 처리 방향으로 진행한다.

## 2. Domain Rules
- Rule 1: access token이 만료되면 secure endpoint 인증 단계에서 `TOKEN_EXPIRED` 성격의 401을 반환한다.
- Rule 2: access token 형식, 서명, 파싱이 유효하지 않으면 secure endpoint 인증 단계에서 `TOKEN_INVALID` 성격의 401을 반환한다.
- Rule 3: 토큰 subject에 해당하는 사용자가 더 이상 존재하지 않으면 리소스 조회 실패가 아니라 인증 실패로 간주해 401을 반환한다.
- Mutable Rules:
  - 탈퇴/삭제 사용자 토큰의 상세 에러 코드는 `TOKEN_INVALID`로 통일할 수 있다.
- Immutable Rules:
  - secure endpoint는 유효한 인증 없이는 컨트롤러 로직에 진입하지 않는다.
  - 인증 실패는 5xx 또는 gateway 오류로 반환하지 않는다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 유효한 access token으로 내 정보 조회.
  - Trigger: `GET /api/users/me` 또는 `GET /api/student/profile`.
  - Actor: 로그인 사용자.
  - Steps:
    - 클라이언트가 `Authorization: Bearer <valid acToken>`을 보낸다.
    - JWT 필터가 토큰을 파싱하고 사용자 정보를 로드한다.
    - 컨트롤러가 인증된 사용자 기준으로 응답을 반환한다.
  - Expected Result: 기존 정상 응답을 유지한다.

### Exception / Boundary Flow
- Scenario Name: 만료된 access token.
  - Condition: JWT expiration이 지난 access token으로 secure endpoint를 호출한다.
  - Expected Behavior: 401과 `TOKEN_EXPIRED` 계열 에러 응답을 반환한다.
- Scenario Name: 형식/서명이 유효하지 않은 access token.
  - Condition: 파싱할 수 없거나 서명이 맞지 않는 access token으로 secure endpoint를 호출한다.
  - Expected Behavior: 401과 `TOKEN_INVALID` 계열 에러 응답을 반환한다.
- Scenario Name: 탈퇴/삭제 사용자 access token.
  - Condition: JWT subject는 UUID 형식이지만 사용자 저장소에 해당 사용자가 없다.
  - Expected Behavior: 401과 `TOKEN_INVALID` 계열 에러 응답을 반환한다.

## 4. Transaction / Consistency
- Transaction Start Point: 없음. 인증 필터 처리 범위다.
- Transaction End Point: 없음.
- Atomicity Scope: 인증 실패 시 `SecurityContextHolder`에 인증 상태가 남지 않아야 한다.
- Eventual Consistency Allowed: 해당 없음.

## 5. API List
- Endpoint:
  - Method: `GET`
  - Request DTO: 없음.
  - Response DTO: 기존 `ErrorResponse`.
  - Authorization: Bearer access token 필요.
  - Idempotency: 해당 없음.
- Endpoint:
  - Method: `GET /api/student/profile`
  - Request DTO: 없음.
  - Response DTO: 기존 `ErrorResponse`.
  - Authorization: Bearer access token 필요.
  - Idempotency: 해당 없음.

## 6. Exception Policy
- Error Code:
  - Condition: access token 만료.
  - Message Convention: 기존 `TOKEN_EXPIRED` 메시지 사용.
  - Handling Layer: Global/Security.
  - User Exposure: 401 JSON 에러 응답.
- Error Code:
  - Condition: access token invalid 또는 사용자 로딩 실패.
  - Message Convention: 기존 `TOKEN_INVALID` 메시지 사용.
  - Handling Layer: Global/Security.
  - User Exposure: 401 JSON 에러 응답.

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Domain complete. 변경 없음.
- [ ] Phase 3 Application complete. 변경 없음.
- [ ] Phase 4 Infrastructure complete. 변경 없음.
- [x] Phase 5 Global/Config complete
- [x] Phase 6 API/Controller complete. 컨트롤러 변경 없음, security chain 테스트로 검증.

## 8. Generated File List
- Path: `docs/specs/20260614-issue-261-secure-invalid-token-401/spec.md`
  - Description: issue 261 버그 수정 범위와 인증 실패 정책.
  - Layer: Context.
- Path: `docs/specs/20260614-issue-261-secure-invalid-token-401/clarify.md`
  - Description: 확인된 결정과 리스크.
  - Layer: Context.
- Path: `docs/specs/20260614-issue-261-secure-invalid-token-401/plan.md`
  - Description: global/security 변경 및 테스트 전략.
  - Layer: Context.
- Path: `docs/specs/20260614-issue-261-secure-invalid-token-401/tasks.md`
  - Description: 실행 체크리스트와 테스트 로그.
  - Layer: Context.
- Path: `docs/specs/20260614-issue-261-secure-invalid-token-401/checklist.md`
  - Description: 세부 작업 체크리스트.
  - Layer: Context.
- Path: `docs/specs/20260614-issue-261-secure-invalid-token-401/context-notes.md`
  - Description: 작업 중 결정과 근거 기록.
  - Layer: Context.
