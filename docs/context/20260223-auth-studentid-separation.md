# Context: UserDetails StudentId 제거 및 인증/도메인 책임 분리

## 1. Feature Overview (Required)
- Purpose: 인증 스냅샷(`CustomUserDetails`)에서 변동 가능한 도메인 식별자(`studentId`)를 제거하고, 학생 식별은 요청 시점에 도메인에서 해석하도록 분리해 인증 캐시와 도메인 상태 결합을 해소한다.
- Scope:
  - In:
    - `global.security.CustomUserDetails`에서 `studentId` 제거.
    - `AuthTokenCache`/`JwtAuthenticationFilter` 경로에서 캐시된 인증 객체가 학생 상태를 보관하지 않도록 정합성 보장.
    - 학생 컨텍스트가 필요한 API(`AcademicRecordController`, `SemesterController`, `GraduationController`, `StudentController`, `SuwonScrapeController`)에서 `userId` 기준 학생 식별 로직으로 전환.
    - 학생 미연결/미존재 예외를 명시적으로 처리하는 정책 정립.
  - Out:
    - JWT 토큰 포맷(클레임 구조) 변경.
    - 포털 크롤링/학업 동기화의 비즈니스 규칙 변경.
    - 학업/졸업 계산 로직 자체 변경.
- Expected Impact: 학생 엔티티 생성/연결/병합 이후에도 인증 캐시 무효화에 의존하지 않고 최신 학생 연결 상태를 반영할 수 있으며, 인증 계층과 도메인 계층의 책임 경계를 명확히 유지한다.
- Stakeholder Confirmation: Requirement provided by requester on 2026-02-23 ("UserDetails의 StudentId 제거 및 인증/도메인 책임 분리").

## 2. Domain Rules (Highest Priority, Required)
- Rule 1: 인증 컨텍스트는 요청 주체 식별 정보(최소 `userId`)만 책임지며, 학생 식별자(`studentId`)를 캐시 가능한 인증 객체에 포함하지 않는다.
- Rule 2: 학생 컨텍스트가 필요한 유스케이스는 매 요청마다 `userId -> Student`를 도메인에서 조회해 최신 연결 상태를 사용한다.
- Rule 3: Student 생성/연결/병합 등 도메인 상태 변화는 인증 캐시 무효화가 없어도 다음 요청부터 즉시 반영되어야 한다.
- Rule 4: 학생 연결이 없는 사용자의 학생 전용 기능 접근은 도메인 예외 정책(`USER_NOT_CONNECTED`)으로 일관되게 실패해야 하며, `NullPointerException` 등 기술 예외로 노출되면 안 된다.

- Mutable Rules:
  - 학생 컨텍스트 조회 구현체(헬퍼/서비스/리졸버)와 배치 위치는 변경 가능하다.
  - 인증 객체에 포함할 최소 필드(email, profile 등)는 향후 필요에 따라 조정 가능하다.
- Immutable Rules:
  - `studentId`는 인증 캐시 객체(`UserDetails`)에 포함하지 않는다.
  - 학생 식별의 단일 진실원은 도메인(User-Student 연관)이다.
  - 도메인 상태 반영을 위해 인증 캐시 강제 무효화를 필수 전제로 두지 않는다.

## 3. Use-case Scenarios (Required)

### Normal Flow
- Scenario Name: 포털 연동 직후 동일 액세스 토큰으로 학생 기능 접근
  - Trigger: 사용자가 이미 발급받은 액세스 토큰으로 학생 컨텍스트가 필요한 API를 호출한다.
  - Actor: Authenticated User / API Controller / Student Domain
  - Steps:
    1. JWT 인증 필터가 `userId` 중심의 인증 객체를 `SecurityContext`에 설정한다.
    2. 컨트롤러(또는 애플리케이션 서비스)가 `userId`를 사용해 현재 `Student`를 조회한다.
    3. 도메인 서비스가 조회된 최신 `studentId`로 학업/졸업/학생 기능을 수행한다.
  - Expected Result: 인증 캐시 재생성 없이도 최신 Student 연결 상태가 반영된다.

### Exception / Boundary Flow
- Scenario Name: 학생 미연결 사용자의 학생 전용 API 호출
  - Condition: 인증된 사용자의 `User.student`가 null이다.
  - Expected Behavior: 사전에 정의된 비즈니스 예외(`USER_NOT_CONNECTED`)를 반환하고, 내부 기술 예외는 발생시키지 않는다.

- Scenario Name: 데이터 정합성 훼손으로 학생 엔티티 조회 실패
  - Condition: 사용자-학생 연결 참조가 있으나 대상 Student가 존재하지 않는다.
  - Expected Behavior: `STUDENT_NOT_FOUND`를 반환하고 트랜잭션/캐시 상태를 오염시키지 않는다.

## 4. Transaction and Consistency Policy (Required)
- Transaction Start Point: 학생 컨텍스트가 필요한 유스케이스 시작 시점(컨트롤러 진입 후 서비스 계층).
- Transaction End Point: 각 유스케이스 응답 반환 시점.
- Atomicity Scope: 단일 요청 내에서는 동일한 학생 컨텍스트 해석 결과를 기준으로 처리한다.
- Eventual Consistency Allowed: 인증 캐시는 사용자 인증 정보만 보관하므로 학생 상태 일관성에는 eventual consistency를 허용하지 않는다(요청 시점 조회 기준 즉시 일관성).

## 5. API List (Optional / Required When Present)
- Endpoint: `/api/student/profile`
  - Method: `GET`
  - Request DTO: 없음
  - Response DTO: `StudentProfileResponse` (변경 없음)
  - Authorization: JWT 인증 필요
  - Idempotency: Yes
- Endpoint: `/api/student/target-gpa`
  - Method: `POST`
  - Request DTO: `targetGpa` query parameter (변경 없음)
  - Response DTO: `MessageOnlyResponse` (변경 없음)
  - Authorization: JWT 인증 필요
  - Idempotency: No
- Endpoint: `/api/student/reset`
  - Method: `POST`
  - Request DTO: 없음
  - Response DTO: `MessageOnlyResponse` (변경 없음)
  - Authorization: JWT 인증 필요
  - Idempotency: No
- Endpoint: `/api/academic/record`, `/api/academic/summary`, `/api/academic/semester`, `/api/academic/semesters`, `/api/graduation/progress`, `/api/suwon-scrape/refresh`
  - Method: 기존과 동일
  - Request DTO: 변경 없음
  - Response DTO: 변경 없음
  - Authorization: JWT 인증 필요
  - Idempotency: 기존 정책 유지

## 6. Exception Policy (Required)
- Error Code: `USER_NOT_CONNECTED`
  - Condition: `userId`로 조회한 사용자에게 연결된 Student가 없음.
  - Message Convention: 기존 `ErrorCode.USER_NOT_CONNECTED` 메시지 사용.
  - Handling Layer: 학생 컨텍스트 해석 계층(서비스/리졸버)에서 즉시 발생.
  - User Exposure: 기존 공통 에러 응답 포맷으로 노출.
- Error Code: `STUDENT_NOT_FOUND`
  - Condition: 학생 참조가 있으나 엔티티 조회 실패.
  - Message Convention: 기존 `ErrorCode.STUDENT_NOT_FOUND` 메시지 사용.
  - Handling Layer: Student 도메인 서비스.
  - User Exposure: 기존 공통 에러 응답 포맷으로 노출.
- Error Code: `AUTHENTICATION_REQUIRED`
  - Condition: 인증 정보 누락/실패.
  - Message Convention: 기존 `ErrorCode.AUTHENTICATION_REQUIRED` 메시지 사용.
  - Handling Layer: Spring Security / 인증 필터 체인.
  - User Exposure: 기존 공통 에러 응답 포맷으로 노출.

## 7. Phase Checklist
- [x] Phase 1 Context: requirements, domain rules, exception policy fixed
- [ ] Phase 2 Domain: models, services, exceptions, pure tests written
- [ ] Phase 3 Application: orchestration, transactions, repository interface validation
- [ ] Phase 4 Infrastructure: persistence, external integration, technical implementation validated
- [ ] Phase 5 Global/Config: configuration, security, logging impact reviewed
- [ ] Phase 6 API/Controller: endpoints, docs, validation flows confirmed

## 8. Generated File List (Required)
- Path: docs/context/20260223-auth-studentid-separation.md
  - Description: 인증 컨텍스트에서 StudentId 제거 및 학생 컨텍스트 조회 책임 분리를 위한 Context 문서.
  - Layer: Context documentation
