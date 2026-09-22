# 사용자 문의 작성 및 본인 문의 조회 구현 계획

**Goal:** 사용자 문의 생성·본인 목록·상세 API와 생성 시점 사용자·학적 스냅샷을 구현한다.

**Architecture:** `ReportService`가 인증 사용자와 학적을 한 번 조회하고 `GraduationMajorResolver`로 요건 상태를 결정한 뒤 `Report`에 스냅샷을 저장한다. `ReportRepository`는 소유자별 최신순 페이지 조회와 계정 수명주기용 일괄 update를 담당한다. Controller는 인증 주체만 서비스에 전달하고 Springdoc 인터페이스로 공개 계약을 분리한다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Flyway, JUnit 5, Mockito, MockMvc, Springdoc.

**Spec:** [design.md](design.md).

## 공통 제약

- 브랜치는 `feat/346`이며 `dev`에서 생성했다.
- 기존 미추적 사용자 파일은 수정하거나 포함하지 않는다.
- 적용된 migration은 수정하지 않고 구현 시점의 다음 version인 V15를 추가한다.
- migration은 이전 Lambda가 모르는 새 테이블만 추가하는 additive 변경이다.
- 관리자 API, 알림, 첨부파일, 문의 수정·삭제는 제외한다.
- 공개 API와 DB·도메인·아키텍처가 바뀌므로 Springdoc·계약 테스트·Wiki를 함께 갱신한다.

## 파일 경계

| 처리 | 파일 | 역할 |
| --- | --- | --- |
| 추가 | `src/main/resources/db/migration/V15__create_reports.sql` | reports 테이블, CHECK, FK, 복합 인덱스. |
| 추가 | `src/main/java/com/chukchuk/haksa/domain/report/model/*` | Report, 상태 enum, 사용자·학적 스냅샷. |
| 추가 | `src/main/java/com/chukchuk/haksa/domain/report/repository/ReportRepository.java` | 사용자별 페이지 조회와 병합·탈퇴 일괄 update. |
| 추가 | `src/main/java/com/chukchuk/haksa/domain/report/service/ReportService.java` | 생성·조회·졸업요건 상태 판정. |
| 추가 | `src/main/java/com/chukchuk/haksa/domain/report/service/ReportLifecycleService.java` | 계정 병합 소유권 이전과 탈퇴 스냅샷 익명화. |
| 추가 | `src/main/java/com/chukchuk/haksa/domain/report/dto/ReportDto.java` | 작성·목록·상세·페이지 계약. |
| 추가 | `src/main/java/com/chukchuk/haksa/domain/report/controller/ReportController.java` | `/api/reports` HTTP endpoint. |
| 추가 | `src/main/java/com/chukchuk/haksa/domain/report/controller/docs/ReportControllerDocs.java` | Springdoc API 계약. |
| 추가 | `src/main/java/com/chukchuk/haksa/domain/report/wrapper/*` | 성공 응답 OpenAPI wrapper. |
| 수정 | `src/main/java/com/chukchuk/haksa/domain/user/service/UserService.java` | 계정 병합·탈퇴 트랜잭션에 report 수명주기 연결. |
| 수정 | `src/main/java/com/chukchuk/haksa/global/exception/code/ErrorCode.java` | report not-found 오류. |
| 추가 | `src/test/java/com/chukchuk/haksa/domain/report/**` | 모델·서비스·repository·controller·수명주기 테스트. |
| 수정 | `src/test/java/com/chukchuk/haksa/global/db/FlywayMigrationTest.java` | V15 schema·제약·인덱스 검증. |
| 수정 | `src/test/java/com/chukchuk/haksa/global/config/OpenApiResponseContractTest.java` | 세 endpoint 인증·schema 계약. |
| 수정 | Wiki `API-and-Authentication.md` | API와 403/404 계약. |
| 수정 | Wiki `Core-Domain-Flows.md` | 생성·스냅샷·병합·탈퇴 흐름. |
| 수정 | Wiki `Project-Architecture.md` | report 도메인과 reports 테이블. |

## Task 1. migration과 도메인 불변식을 테스트 우선으로 구현한다.

- [x] `ReportTest`에 신규 PENDING 상태, 답변 상태 전환, 빈 답변 거부, 스냅샷 익명화 테스트를 추가했다.
- [x] `FlywayMigrationTest`의 expected version을 V15로 올리고 fresh/V14→V15 migration 테스트를 추가했다.
- [x] `V15__create_reports.sql`에 테이블·FK·CHECK·`(user_id, created_at DESC, id DESC)` 인덱스를 추가했다.
- [x] Report와 enum·embeddable snapshot을 구현해 모델 테스트를 통과시켰다.
- [x] JDBC metadata와 H2 information schema로 FK 삭제 규칙, CHECK 거부, 인덱스 컬럼 순서·방향을 검증했다.

## Task 2. 작성·조회 서비스와 repository를 구현한다.

- [x] `ReportServiceUnitTests`에 학생 부재 UNKNOWN, resolver 성공 AVAILABLE, G02 NOT_AVAILABLE, 다른 예외 전파, 404·403을 추가했다.
- [x] `ReportRepositoryIntegrationTest`에 사용자 격리, `createdAt DESC, id DESC`, tie-break와 페이지 계약을 추가했다.
- [x] 사용자 연관 정보를 fetch하는 기존 `UserRepository.findProfileByIdWithAssociations`를 재사용했다.
- [x] `ReportRepository`의 페이지 query와 `ReportService`의 생성·목록·상세 매핑을 구현했다.
- [x] 목록은 명시적 페이지 DTO로 반환하고 본문·답변·스냅샷을 제외했다.

## Task 3. 계정 병합과 탈퇴 수명주기를 연결한다.

- [x] `UserServiceIntegrationTest`의 실제 문의 행으로 owner 이전 시 submitted snapshot 유지, 탈퇴 시 텍스트 유지·구조화 snapshot 제거를 검증했다.
- [x] repository bulk update와 lifecycle service를 구현했다.
- [x] `UserService.tryMergeWithExistingUser`의 기존 사용자 삭제 전에 owner 이전을 호출했다.
- [x] `UserService.deleteUserById`에서 사용자 soft delete 전에 report snapshot 익명화를 호출했다.
- [x] 기존 사용자 병합·탈퇴 테스트가 회귀하지 않는지 확인했다.

## Task 4. API와 Springdoc 계약을 구현한다.

- [x] `ReportControllerApiIntegrationTest`와 실제 HTTP 테스트에 201·Location, validation 400, 목록, 상세, 404, 403 정보 비노출, 미인증 401을 추가했다.
- [x] Report DTO, Controller, Springdoc docs, 성공 wrapper를 구현했다.
- [x] `page >= 0`, `1 <= size <= 100`을 검증했다.
- [x] `OpenApiResponseContractTest`에 세 보호 endpoint, 201/200 wrapper, 400/401/403/404, request schema와 multipart 부재를 추가했다.

## Task 5. 통합 검증과 Wiki를 완료한다.

- [x] 관련 report·user·migration·OpenAPI 테스트를 실행했다.
- [x] `./gradlew spotlessApply --no-daemon`을 실행하고 요청 범위 밖 포맷 변경이 없는지 확인했다.
- [x] `./gradlew check --stacktrace --no-daemon`을 통과시켰다.
- [x] random port 애플리케이션에서 JWT를 사용해 세 API와 `/v3/api-docs`를 실제 HTTP로 검증했다.
- [x] Wiki의 API, 핵심 흐름, 아키텍처 문서를 갱신·게시하고 `git diff --check`를 확인했다.
- [x] 구현에 참여하지 않은 별도 컨텍스트로 독립 검증을 수행하고 권고사항을 테스트에 반영했다.

## 복구와 남은 위험

- 적용 전 실패는 V15 파일과 코드 변경을 함께 수정한 뒤 재검증한다.
- 적용된 V15에서 문제가 발견되면 V15를 수정·삭제하거나 자동 rollback하지 않고 다음 version의 forward migration으로 보정한다.
- offset pagination은 조회 사이 삽입으로 페이지 이동이 생길 수 있으며 이번 범위에서 허용한다.
- 문의 free text 자체에 사용자가 개인정보를 적을 수 있으므로 구조화 스냅샷 익명화가 본문 전체의 개인정보 제거를 보장하지 않는다. 로그·오류에는 본문을 남기지 않는다.
- CS 기록의 최종 보존 기간은 후속 관리자 기능에서 확정한다.
