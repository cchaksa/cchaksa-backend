# 편입생 영역별 학점 및 지정과목 이수 현황 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: 구현 착수 시 `superpowers:executing-plans`를 사용한다. 서비스와 평가기의 한 코드 경로를 순서대로 바꾸므로 단일 구현 흐름으로 진행하고, 완료 전 독립 Sol 검토를 수행한다.

**Goal:** 3학년 편입생에게 편입 연도보다 2년 앞선 정규 코호트의 전핵·전선 요구학점 50%를 적용해 영역별 학점 현황을 제공한다.

**Architecture:** `TransferGraduationAnalysisService`가 기존 `GraduationMajorResolver`와 `GraduationQueryRepository`로 적용 코호트의 학과 요건을 조회해 `TransferAreaEvaluator.Requirements`로 변환한다. 평가기는 전핵·전선을 동일한 학점 비교 방식으로 계산하고, 기준이 없는 영역만 `UNAVAILABLE`로 둔다. 기존 유효 이수·지정과목·총학점·GPA·외국어 인증과 일반 재학생 경로는 유지한다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Gradle, PostgreSQL, JUnit 5, AssertJ, Mockito, Springdoc.

**Spec:** [design.md](design.md).

## 공통 제약

- 현재 브랜치는 `feat/341`이다.
- 지원 정책은 3학년 편입이다. 저장된 입학/편입 연도에서 2를 뺀 값을 `cohortYear`로 사용하며, 동기화 뒤 현재 `gradeLevel`이 4로 바뀌어도 이 계산은 달라지지 않는다. 2026년 편입은 2024년 요건을 사용한다.
- `department_area_requirements`와 기존 캐시 조회만 사용한다. DB schema와 migration은 변경하지 않는다.
- 전핵·전선은 각각 정규 코호트 요구학점의 정확한 50%를 `BigDecimal`로 계산하며 반올림하지 않는다.
- 전핵 전체 과목 목록은 만들지 않는다. 공개 `requiredCourses`는 호환성을 위해 빈 목록을 유지한다.
- 최종 졸업 판정·추가 졸업심사·수동 입력 기능은 제외한다. 기존 총학점·GPA·외국어 인증, 지정과목, 기타 영역 취득학점과 일반 재학생 동작을 보존한다.
- 복수전공에는 단일전공 절반 정책을 확장하지 않는다. 적용 근거가 추가되기 전까지 전핵·전선 기준만 `UNAVAILABLE`로 둔다.
- 요구사항 부재로 바꾸는 예외는 `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND`뿐이다. 다른 예외는 삼키지 않는다.
- 후속 제품 구현은 공개 API와 도메인 기준을 다루므로 HIGH로 검증한다. 코드 리뷰 그래프를 확인하고 실제 API 검증 뒤 구현에 참여하지 않은 Sol 리뷰를 받는다.
- 커밋은 `341 {type}: {한국어 메시지}` 형식을 사용한다.

## 파일 경계

| 처리 | 파일 | 역할 |
| --- | --- | --- |
| 수정 | `src/main/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisService.java` | 코호트 계산, 전공 별칭 해석, 요건 조회와 `Requirements` 조립을 담당한다. |
| 수정 | `src/main/java/com/chukchuk/haksa/domain/graduation/policy/TransferAreaEvaluator.java` | 전핵 과목 목록 기준을 제거하고 전핵·전선 학점 임계값을 같은 방식으로 비교한다. |
| 수정 | `src/main/java/com/chukchuk/haksa/domain/graduation/dto/TransferAreaProgressDto.java` | `requiredCourses`의 호환성 필드 설명을 현재 계약에 맞춘다. |
| 참조 | `src/main/java/com/chukchuk/haksa/domain/graduation/dto/TransferManualReviewReason.java` | 편입 학년 미확인 사유의 서비스 사용만 제거하고 enum은 호환성을 위해 보존했다. |
| 테스트 | `src/test/java/com/chukchuk/haksa/domain/graduation/policy/TransferAreaEvaluatorTest.java` | 전핵·전선 절반 임계값과 영역별 누락을 검증한다. |
| 테스트 | `src/test/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisServiceTests.java` | 2026→2024 조회, resolver·repository 연결, 예외·누락·복수전공 처리를 검증한다. |
| 테스트 | `src/test/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisIntegrationTest.java` | 실제 repository를 포함한 편입 응답과 일반 경로 회귀를 검증한다. |
| 테스트 | `src/test/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationProgressHttpIntegrationTest.java` | random port와 JWT로 실제 `/v3/api-docs`와 졸업 진행 GET을 검증한다. |
| 테스트 | `src/test/java/com/chukchuk/haksa/domain/graduation/controller/GraduationControllerApiIntegrationTest.java`, `src/test/java/com/chukchuk/haksa/global/config/OpenApiResponseContractTest.java` | 실제 HTTP 응답과 OpenAPI 계약을 검증한다. |

새 저장소, 새 정책 서비스, 새 테이블은 추가하지 않는다.

## Task 1. 전핵·전선 학점 비교 정책을 고정한다.

**Files:** `TransferAreaEvaluatorTest.java`, `TransferAreaEvaluator.java`, `TransferAreaProgressDto.java`.

**입출력:** `Requirements`는 nullable `BigDecimal coreRequiredCredits`, nullable `BigDecimal electiveRequiredCredits`, 영역별 미확인 사유를 가진다. `evaluate`는 두 영역의 개인 취득학점을 각 임계값과 비교한다.

- [x] 전핵과 전선의 기준 미만·같음·초과가 false·true·true인지 테스트한다.
- [x] 홀수 원래 요구학점의 절반을 반올림하지 않고 `BigDecimal` 소수로 유지하는 테스트를 추가한다.
- [x] 전핵 또는 전선 기준이 하나만 없을 때 해당 영역만 `UNAVAILABLE`인지 검증한다.
- [x] 기타 영역은 `EARNED_ONLY`이고 전취가 전핵에 합쳐지지 않는 기존 동작을 유지한다.
- [x] `Requirements.coreCourses`와 `RequiredCourse`를 제거하고 전핵·전선에 같은 학점 비교 규칙을 적용한다. 두 영역의 `requiredCourses`는 빈 목록이다.
- [x] `TransferAreaProgressDto.requiredCourses`의 Springdoc 설명을 호환성 유지 빈 목록으로 고친다.
- [x] 관련 정책 테스트를 통과시킨다.

## Task 2. 적용 코호트 요건을 서비스에 연결한다.

**Files:** `TransferGraduationAnalysisService.java`, `TransferGraduationAnalysisServiceTests.java`, 기존 resolver·repository 테스트.

**입출력:** private 요건 조회 메서드는 `Student`를 받아 `Requirements`를 반환한다. 성공 경로는 `admissionYear - 2`, resolver가 반환한 primary major ID, `getAreaRequirementsWithCache` 결과를 사용한다.

- [x] 입학/편입 연도 2026인 단일전공 학생이 2024 요건을 조회하는 테스트를 추가한다.
- [x] 기존 `GraduationMajorResolver`를 통해 `establishedDepartmentName` 별칭 후보의 요건을 조회하는 통합 테스트를 추가한다.
- [x] 전핵·전선 원래 요구학점을 각각 정확한 절반으로 `Requirements`에 전달한다.
- [x] 전핵 또는 전선 행이 없을 때 다른 영역 기준을 보존하는 테스트를 추가한다.
- [x] 같은 영역에 같은 요구학점 행이 중복되면 그 기준을 사용하고, 서로 다른 요구학점이 중복되면 해당 영역만 `UNAVAILABLE`인지 DB 통합 테스트로 검증한다.
- [x] 학적 연도·주전공·요건이 없을 때 응답을 유지하고 전핵·전선을 `UNAVAILABLE`로 둔다.
- [x] 복수전공에는 단일전공 요건을 적용하지 않고 기존 나머지 계산을 유지한다.
- [x] `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND`만 unavailable로 변환하고 다른 예외는 전파한다.
- [x] `Requirements.unavailable()` 고정 전달을 실제 조회 결과로 교체한다.
- [x] 항상 추가하던 `TRANSFER_ENTRY_GRADE_UNKNOWN` 수동 확인 사유를 제거한다.
- [x] 관련 서비스·resolver·repository 테스트를 통과시킨다.

## Task 3. 통합·공개 API·일반 재학생 회귀를 검증한다.

**Files:** 편입 통합 테스트, controller API 통합 테스트, OpenAPI 계약 테스트, controller docs, 이 계획의 실행 기록.

- [x] 테스트 DB의 2024 `department_area_requirements`로 2026 편입생이 2024 기준 절반을 반환하는 통합 테스트를 추가한다.
- [x] 전핵 또는 전선 행을 하나씩 누락한 fixture로 영역별 독립 `UNAVAILABLE`을 검증한다.
- [x] 비편입 학생의 기존 계산 경로 회귀 테스트를 유지하고 실행한다.
- [x] random port 애플리케이션과 JWT를 사용해 실제 `/v3/api-docs`와 `GET /api/graduation/progress` HTTP 경로를 검증한다.
- [x] JSON·Springdoc 계약에서 `requiredCourses`가 빈 목록이며 현재 전핵 학점 비교 의미로 설명되는지 검증한다.
- [x] `./gradlew spotlessApply --no-daemon` 후 요청 범위 밖 포맷 변경이 없는지 확인한다.
- [x] `./gradlew check --stacktrace --no-daemon`을 최종 통과시킨다.
- [x] 코드 리뷰 그래프를 확인하고 구현 diff·테스트·실제 API 증거를 구현에 참여하지 않은 Sol 리뷰어에게 제공한다. 차단사항을 수정한 뒤 영향받는 검증과 최종 `check`를 다시 실행한다.

## Task 4. Wiki와 완료 증거를 갱신한다.

**Files:** 별도 Wiki 저장소 `master`의 `API-and-Authentication.md`, `Core-Domain-Flows.md`, `Troubleshooting.md`, 이 계획의 실행 기록.

- [x] Wiki에 3학년 편입 코호트 계산, 전핵·전선 각각 50%, 정확한 소수 임계값, `requiredCourses=[]`, 영역별 `UNAVAILABLE`, 최종 졸업 판정 제외를 기록한다.
- [x] 복수전공 편입 정책이 확인되지 않아 전핵·전선 비교를 제공하지 않는 현재 제한을 기록한다.
- [x] `git diff --check`와 문서 링크·변경 범위를 검토하고 실제 명령, 결과, 독립 리뷰 결론, Wiki 커밋을 실행 기록에 남긴다.

## 성공 기준 추적

| 설계 성공 기준 | Task |
| --- | --- |
| 2026→2024 코호트와 학과 개편 별칭 | 2, 3 |
| 전핵·전선 50%, 미만·같음·초과와 홀수 절반 | 1, 2, 3 |
| 영역별 독립 기준 누락 | 1, 2, 3 |
| 동일·상충 요구학점 중복 처리 | 2, 3 |
| 연도·전공·전체 요건 누락과 복수전공 제한 | 2, 3 |
| 기타 영역·지정과목·총학점·GPA·외국어 보존 | 2, 3 |
| 일반 재학생 회귀와 실제 HTTP 검증 | 3 |
| Wiki와 독립 Sol 검토 | 3, 4 |

## 실행 기록

- 2026-09-06. `feat/341`에서 노션 기준 설계를 작성했다.
- 2026-09-07. 편입 영역 응답, 유효 수강 정규화, 지정과목 학점, 최종 판정 및 수동 PATCH 제거를 구현했다. 당시 전핵·전선 원천 결정 전이라 `Requirements.unavailable()`을 연결했다.
- 2026-09-07. Java 17에서 전체 `check`와 정책·DTO·서비스·컨트롤러·OpenAPI·편입 통합 테스트를 통과했다. DB schema와 migration은 변경하지 않았다.
- 2026-09-08. PR #343 CodeRabbit 리뷰에 따라 지정과목 이수와 개인 학점 가용성을 분리하고 과목코드 정규화를 `Locale.ROOT`로 고쳤다. Java 17에서 관련 테스트와 최종 `check`, `git diff --check`를 통과했다.
- 2026-09-08. 사용자가 이전의 적용 연도·전핵 전체 목록 차단 결정을 대체했다. 3학년 편입 연도에서 2를 뺀 정규 코호트의 기존 `department_area_requirements` 전핵·전선 요구학점을 각각 정확히 50% 적용하기로 확정했다.
- 2026-09-08. 후속 구현에서 2026→2024, 임계값, 영역별 누락, 학과 개편 별칭, 비편입 회귀 테스트를 통과했다. random port와 JWT를 사용한 실제 `/v3/api-docs` 및 졸업 진행 GET도 통과했고, 기존 schema 설명 불일치는 현재 계약에 맞게 수정했다.
- 2026-09-08. 독립 Sol 리뷰에서 상충 기준 행의 임의 선택을 확인했다. DB 통합 테스트가 기존 `COMPARISON` 응답으로 실패하는 것을 확인한 뒤, 동일 요구학점 중복은 허용하고 상충 값은 해당 영역만 `UNAVAILABLE`로 수정했다. 재검토에서 차단사항 없음으로 확인됐다.
- 2026-09-08. Java 17에서 `./gradlew spotlessApply test --tests '*TransferGraduationAnalysisIntegrationTest.leavesConflictingRequirementUnknownAndAcceptsIdenticalDuplicates' --no-daemon --offline`을 통과했다. 최종 `./gradlew check --stacktrace --no-daemon`은 491건 중 490건 통과·1건 건너뜀·실패와 오류 0건이다. 새 HTTP 테스트는 로컬 랜덤 포트 서버에서 테스트 JWT로 인증한 GET과 `/v3/api-docs`를 검증하며 운영 데이터나 서비스는 사용하지 않는다.
- 2026-09-08. Wiki `master`의 `API-and-Authentication`, `Core-Domain-Flows`, `Troubleshooting`에 PR #343 적용 예정 계약을 기록했다. Wiki 커밋은 `bb4d51d`이며, 실제 운영 반영 여부는 PR과 배포 이력으로 구분한다. DB schema와 migration은 이번 연결 작업에서 변경하지 않았다.
- 2026-09-08. 최종 `git diff --check`와 문서 링크·변경 범위 검토를 통과했다. 기존 `.DS_Store`는 커밋하지 않는다. 복수전공 편입 정책과 이전부터 남은 동일 학기 상충 수강 기록 문제는 별도 후속 범위이며, 운영 배포 및 프론트 화면 연동은 이번 검증에 포함하지 않았다.
- 2026-09-08. 사용자가 복수전공 편입생을 이번 범위에서 제외하고 수강 기록 충돌 수정을 승인했다. 단일 평가기의 제한된 수정으로 최신 여부를 연도·학기로만 비교하고, 최신 학기 안의 개인 학점·성적등급·이수구분 차이를 충돌로 처리한다. 더 최신 학기로 교체할 때 과거 충돌을 해제하며 같은 학기의 일치 기록만으로는 해제하지 않는다.
- 2026-09-08. `./gradlew test --tests '*TransferCourseEvaluatorTest' --no-daemon`에서 점수 차이에 가려진 충돌 3건과 과거 충돌 잔존 2건의 실패를 먼저 확인했다. 수정 뒤 `./gradlew spotlessApply test --tests '*TransferCourseEvaluatorTest' --tests '*TransferAreaEvaluatorTest' --tests '*DesignatedCourseEvaluatorTest' --no-daemon`을 통과했다. 같은 해 다음 학기와 다음 연도를 각각 입력 순서 6가지로 검증했다.
- 2026-09-08. Java 17에서 최종 `./gradlew check --stacktrace --no-daemon`은 497건 중 496건 통과·1건 건너뜀·실패와 오류 0건이다. 코드 리뷰 그래프를 갱신하고 diff와 `git diff --check`를 확인했다. Wiki `Core-Domain-Flows`의 PR #343 절에 최신 학기·충돌 처리 규칙을 추가했다. 공개 API 구조·DB schema와 복수전공 정책은 변경하지 않았다.
