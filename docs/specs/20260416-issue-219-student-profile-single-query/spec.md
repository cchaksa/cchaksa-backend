# Issue 219: `/api/student/profile` 단일 조회 전환

## 1. Feature Overview
- Purpose:
  - `/api/student/profile`만 기존 `userId -> studentId -> profile` 2단계 조회에서 `userId -> profile` 단일 조회로 전환한다.
- Scope
  - In:
    - `StudentController#getProfile`의 호출 경로 변경
    - `StudentService#getStudentProfileByUserId(UUID userId)` 추가
    - `UserRepository` 프로필 전용 fetch query 추가
    - 프로필 DTO null-safe 보강
    - 프로필 API 문서와 예시값 정합성 수정
    - 관련 단위/API 테스트 보강
  - Out:
    - `target-gpa`, `reset`, 학업/졸업 API의 학생 컨텍스트 해석 방식 변경
    - JWT 클레임, `CustomUserDetails`, `AuthTokenCache` 구조 변경
    - 학생 도메인 계산 규칙, 포털 연동 로직 변경
- Expected Impact:
  - 인증 객체에 `studentId`를 넣지 않으면서도 `/api/student/profile`이 요청 시점의 최신 `User-Student` 연결 상태를 직접 반영한다.
  - 프로필 API 조회 시 불필요한 2단계 조회를 제거하고, 문서/테스트와 실제 예외 의미를 일치시킨다.
- Stakeholder Confirmation:
  - Legacy context: `docs/context/20260223-auth-studentid-separation.md`
  - Implementation approval: requester explicitly asked to implement this plan on 2026-04-16 and requested no commit before review.

## 2. Domain Rules
- Rule 1:
  - 인증 컨텍스트는 `userId`만 신뢰 가능한 식별자로 제공하고, `studentId`는 인증 객체나 캐시에 포함하지 않는다.
- Rule 2:
  - `/api/student/profile`은 매 요청마다 `userId` 기준으로 현재 `User-Student` 연결 상태를 조회해 응답을 만든다.
- Rule 3:
  - `user`가 없으면 `USER_NOT_FOUND`, `user`는 있으나 `student`가 없으면 `USER_NOT_CONNECTED`를 반환한다.
- Mutable Rules:
  - 프로필 조회를 위한 fetch query의 배치 위치와 메서드 이름
  - DTO 조립 시 null-safe 처리 방식
- Immutable Rules:
  - 외부 API contract는 유지한다.
  - 다른 학생 컨텍스트 API는 이번 이슈 범위 밖이다.
  - 인증/도메인 책임 분리 원칙은 유지한다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 로그인 사용자의 프로필 단일 조회
  - Trigger:
    - JWT 인증된 사용자가 `GET /api/student/profile`을 호출한다.
  - Actor:
    - Authenticated User / StudentController / StudentService / UserRepository
  - Steps:
    1. 컨트롤러가 `userDetails.getId()`를 읽는다.
    2. 서비스가 `userId` 기준으로 `user`, `student`, `department`, `major`, `secondaryMajor`를 한 번에 조회한다.
    3. 서비스가 기존 응답 스키마 그대로 `StudentProfileResponse`를 생성한다.
  - Expected Result:
    - 응답 DTO shape는 그대로 유지되고, 현재 연결 상태 기준 프로필이 반환된다.

### Exception / Boundary Flow
- Scenario Name: 존재하지 않는 사용자
  - Condition:
    - `userId`에 해당하는 `User`가 없다.
  - Expected Behavior:
    - `USER_NOT_FOUND`를 반환한다.
- Scenario Name: 학생 미연결 사용자
  - Condition:
    - `User`는 있으나 `student` 연관이 없다.
  - Expected Behavior:
    - `USER_NOT_CONNECTED`를 반환한다.
- Scenario Name: 프로필 타임스탬프 null
  - Condition:
    - `student.updatedAt` 또는 `user.lastSyncedAt`가 null이다.
  - Expected Behavior:
    - 응답 필드는 빈 문자열로 내려가고 기술 예외가 발생하지 않는다.

## 4. Transaction / Consistency
- Transaction Start Point:
  - `StudentService#getStudentProfileByUserId(UUID userId)` 진입 시점
- Transaction End Point:
  - 프로필 DTO 반환 시점
- Atomicity Scope:
  - 단일 요청 내 프로필 조회와 DTO 조립
- Eventual Consistency Allowed:
  - 불가. 요청 시점의 `User-Student` 연결 상태를 즉시 반영해야 한다.

## 5. API List (필요 시)
- Endpoint:
  - Method: `GET`
  - Request DTO: 없음
  - Response DTO: `StudentDto.StudentProfileResponse`
  - Authorization: JWT 인증 필요
  - Idempotency: Yes

## 6. Exception Policy
- Error Code: `USER_NOT_FOUND`
  - Condition:
    - `userId`에 해당하는 `User`가 없다.
  - Message Convention:
    - 기존 `ErrorCode.USER_NOT_FOUND` 메시지 사용
  - Handling Layer:
    - `StudentService`
  - User Exposure:
    - 기존 공통 에러 응답 포맷
- Error Code: `USER_NOT_CONNECTED`
  - Condition:
    - `User`는 있으나 `student` 연관이 없다.
  - Message Convention:
    - 기존 `ErrorCode.USER_NOT_CONNECTED` 메시지 사용
  - Handling Layer:
    - `StudentService`
  - User Exposure:
    - 기존 공통 에러 응답 포맷

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [x] Phase 2 Domain complete
- [x] Phase 3 Application complete
- [x] Phase 4 Infrastructure complete
- [x] Phase 5 Global/Config complete
- [x] Phase 6 API/Controller complete

## 8. Generated File List
- Path: `docs/specs/20260416-issue-219-student-profile-single-query/spec.md`
  - Description:
    - Issue 219 범위와 도메인 규칙 정의
  - Layer:
    - Context documentation
- Path: `docs/specs/20260416-issue-219-student-profile-single-query/clarify.md`
  - Description:
    - legacy context 이관 기록과 승인 근거
  - Layer:
    - Context documentation
- Path: `docs/specs/20260416-issue-219-student-profile-single-query/plan.md`
  - Description:
    - 레이어별 구현 계획
  - Layer:
    - Context documentation
- Path: `docs/specs/20260416-issue-219-student-profile-single-query/tasks.md`
  - Description:
    - 작업 체크리스트와 테스트 실행 로그
  - Layer:
    - Context documentation
