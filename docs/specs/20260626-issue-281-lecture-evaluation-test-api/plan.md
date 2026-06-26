# Issue 281 Implementation Plan

## 목표

dev/test profile에서 고정 프론트 테스트 계정의 강의평가 상태를 인증 없이 재구성하는 5개 POST API를 추가한다.

## 선택한 접근

`AdminTestController`에 강의평가 테스트 전용 endpoint를 추가하고, 실제 상태 재구성은 신규 `AdminTestLectureEvaluationService`에 분리한다. 기존 `LectureEvaluationService#submit`과 `#skip`은 사용자 요청 검증과 일괄 제출 흐름에 맞춰져 있어 전체 삭제 후 재구성, `empty-semester`, 실제 강의 재사용 seed에는 맞지 않으므로 재사용하지 않는다.

## 수정 파일

- Modify: `src/main/java/com/chukchuk/haksa/domain/admin/controller/AdminTestController.java`.
- Modify: `src/main/java/com/chukchuk/haksa/domain/admin/controller/docs/AdminTestControllerDocs.java`.
- Create: `src/main/java/com/chukchuk/haksa/domain/admin/service/AdminTestLectureEvaluationService.java`.
- Modify: `src/main/java/com/chukchuk/haksa/domain/academic/record/model/SemesterAcademicRecord.java`.
- Modify: `src/main/java/com/chukchuk/haksa/domain/academic/record/repository/SemesterAcademicRecordRepository.java`.
- Modify: `src/main/java/com/chukchuk/haksa/domain/academic/record/repository/StudentCourseRepository.java`.
- Modify: `src/main/java/com/chukchuk/haksa/domain/course/repository/CourseOfferingRepository.java`.
- Modify: `src/main/java/com/chukchuk/haksa/domain/lectureevaluations/repository/CourseEvaluationRepository.java`.
- Modify: `src/main/java/com/chukchuk/haksa/global/security/SecurityConfig.java`.
- Modify: `src/main/resources/public/openapi.yaml`.
- Modify: `src/test/java/com/chukchuk/haksa/domain/admin/controller/AdminTestControllerApiIntegrationTest.java`.
- Create: `src/test/java/com/chukchuk/haksa/domain/admin/service/AdminTestLectureEvaluationServiceUnitTests.java`.
- Modify: `src/test/java/com/chukchuk/haksa/global/security/filter/JwtAuthenticationFilterTests.java`.
- Modify: `src/test/java/com/chukchuk/haksa/global/config/OpenApiResponseContractTest.java`.

## Task 1: 서비스 단위 테스트를 먼저 추가한다

- `AdminTestLectureEvaluationServiceUnitTests`를 생성한다.
- `empty-semester`가 대상 학기의 평가 데이터, 수강 데이터, 학기 row를 삭제하고 캐시를 삭제하는지 검증한다.
- `not-released`가 실제 교수 연결 `CourseOffering`을 재사용해 IP 수강 과목과 `NOT_RELEASED` 학기 row를 만드는지 검증한다.
- `pending`이 완료 성적 수강 과목과 `PENDING` 학기 row를 만들고 평가 데이터는 만들지 않는지 검증한다.
- `skipped`가 완료 성적 수강 과목과 `SKIPPED` 학기 row를 만들고 평가 데이터는 만들지 않는지 검증한다.
- `completed`가 완료 성적 수강 과목, `COMPLETED` 학기 row, 과목별 평가와 태그를 만드는지 검증한다.
- 후보 `CourseOffering`이 없으면 `CommonException(ErrorCode.INVALID_ARGUMENT)`로 실패하고 임의 강의/교수를 생성하지 않는지 검증한다.

검증 명령:

```bash
./gradlew test --tests 'com.chukchuk.haksa.domain.admin.service.AdminTestLectureEvaluationServiceUnitTests'
```

예상 RED:

- 신규 서비스와 repository 메서드가 없어서 컴파일 또는 테스트 실패.

## Task 2: 상태 재구성 서비스를 구현한다

- `AdminTestLectureEvaluationService`를 `@Service`, `@Transactional`로 추가한다.
- 상수로 target `userId`, `studentId`, `year`, `semester`를 둔다.
- 고정 사용자를 조회하고 연결 학생 ID가 target `studentId`와 일치하는지 확인한다.
- 모든 상태 API는 순행/역행 여부와 관계없이 기존 target 학기 데이터를 먼저 삭제한 뒤 목표 상태를 재구성한다.
- 공통 정리 순서:
  - `CourseEvaluationRepository`에서 target 학생/학기 평가를 삭제한다.
  - `StudentCourseRepository`에서 target 학생/학기 수강 과목을 삭제한다.
  - `SemesterAcademicRecordRepository`에서 target 학생/학기 row를 삭제한다.
- `empty-semester`는 공통 정리 후 추가 row를 만들지 않고 종료한다.
- seed가 필요한 상태는 `CourseOfferingRepository`에서 target 학기 교수 연결 실제 강의 후보를 조회한다.
- seed 과목 수는 최소 1개로 시작하되, repository는 안정적인 정렬로 후보를 가져온다.
- `not-released`는 `GradeType.IP`, 나머지 성적 공개 상태는 `GradeType.A_PLUS`로 `StudentCourse`를 생성한다.
- `SemesterAcademicRecord`에는 테스트용 최소 성적 요약 값을 넣고 상태를 강제 세팅한다.
- `completed`는 각 수강 과목의 course/professor로 `CourseEvaluation`을 만들고 대표 태그를 함께 저장한다.
- 모든 public 메서드 끝에서 `AcademicCache#deleteAllByStudentId`를 호출한다.

검증 명령:

```bash
./gradlew test --tests 'com.chukchuk.haksa.domain.admin.service.AdminTestLectureEvaluationServiceUnitTests'
```

예상 GREEN:

- 서비스 단위 테스트 통과.

## Task 3: 컨트롤러 API와 문서 인터페이스를 추가한다

- `AdminTestController`에 5개 `@PostMapping("/test-lecture-evaluations/...")` 메서드를 추가한다.
- `AdminTestControllerDocs`에 같은 메서드와 `@Operation` 설명을 추가한다.
- 각 endpoint는 body 없이 신규 서비스 메서드를 호출하고 `MessageOnlyResponse`를 반환한다.
- 컨트롤러 테스트에서 5개 endpoint가 서비스 메서드 호출과 성공 메시지를 반환하는지 검증한다.

검증 명령:

```bash
./gradlew test --tests 'com.chukchuk.haksa.domain.admin.controller.AdminTestControllerApiIntegrationTest'
```

## Task 4: security public endpoint를 반영한다

- `SecurityConfig.PUBLIC_ENDPOINTS`에 `/api/admin/test-lecture-evaluations/**`를 추가한다.
- `JwtAuthenticationFilterTests`의 public admin test endpoint fixture에 POST endpoint를 추가한다.
- 토큰 없이 POST 호출이 인증 오류 없이 통과하는지 검증한다.

검증 명령:

```bash
./gradlew test --tests 'com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilterTests'
```

## Task 5: OpenAPI static 문서를 갱신한다

- `src/main/resources/public/openapi.yaml`에 5개 path를 추가한다.
- 모든 path는 `Admin Test` tag, `post`, `200` 성공 응답, `400` 실패 응답을 문서화한다.
- `OpenApiResponseContractTest`에 신규 path 존재 검증을 추가한다.

검증 명령:

```bash
./gradlew test --tests 'com.chukchuk.haksa.global.config.OpenApiResponseContractTest'
```

## Task 6: 통합 검증과 커밋

- 관련 테스트를 한 번 더 묶어서 실행한다.

```bash
./gradlew test --tests 'com.chukchuk.haksa.domain.admin.service.AdminTestLectureEvaluationServiceUnitTests' --tests 'com.chukchuk.haksa.domain.admin.controller.AdminTestControllerApiIntegrationTest' --tests 'com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilterTests' --tests 'com.chukchuk.haksa.global.config.OpenApiResponseContractTest'
```

- 전체 테스트를 실행한다.

```bash
./gradlew test
```

- 통과 후 관련 파일만 stage 한다.
- 커밋 메시지는 repo 규칙에 맞춰 한국어로 작성한다.

```bash
git commit -m "281 feat: 강의평가 상태 테스트 API 추가"
```

## 검토 포인트

- `empty-semester`를 `NOT_RELEASED`로 오해하지 않도록 API명과 문서 표현을 유지한다.
- 실제 강의/교수 재사용 조건을 완화하지 않는다.
- 순행/역행과 관계없이 target 학기 평가 데이터, 수강 데이터, 학기 row를 먼저 삭제한다.
- `CourseEvaluation` unique key 충돌을 피하기 위해 completed 생성 전 target 학기 평가 데이터를 항상 삭제한다.
- dev/test profile 컨트롤러라는 기존 `Admin Test` 경계를 유지한다.
