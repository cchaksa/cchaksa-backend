# dev 전용 프론트 테스트 어드민 API Plan

## 성공 기준

1. `feat/279` 브랜치에서 dev 전용 API 8개를 구현한다.
2. 테스트 계정 생성 API는 access token과 refresh token을 함께 발급한다.
3. 수정 API는 현재 토큰 계정의 학생 데이터만 바꾼다.
4. 변경 후 `./gradlew test`가 통과한다.

## 구현 단위

### 1. 어드민 API 패키지 추가

새 패키지는 `src/main/java/com/chukchuk/haksa/domain/admin` 아래에 둔다.
새 Java 파일의 첫 줄에는 AGENTS 규칙에 따라 한 줄짜리 한국어 역할 주석을 둔다.

예상 파일이다.

- `src/main/java/com/chukchuk/haksa/domain/admin/controller/AdminTestController.java`.
- `src/main/java/com/chukchuk/haksa/domain/admin/dto/AdminTestDto.java`.
- `src/main/java/com/chukchuk/haksa/domain/admin/service/AdminTestAccountService.java`.
- `src/main/java/com/chukchuk/haksa/domain/admin/service/AdminTestOptionService.java`.
- `src/main/java/com/chukchuk/haksa/domain/admin/service/AdminTestMutationService.java`.
- `src/main/java/com/chukchuk/haksa/domain/admin/controller/docs/AdminTestControllerDocs.java`.

### 2. 테스트 계정 생성

컨트롤러 테스트를 먼저 추가한다.

- 테스트 파일은 `src/test/java/com/chukchuk/haksa/domain/admin/controller/AdminTestControllerApiIntegrationTest.java`다.
- `POST /api/admin/test-users`가 `accessToken`, `refreshToken`, `userId`, `studentId`, `email`, `studentCode`를 반환하는지 검증한다.
- 서비스 호출 인자가 요청 바디와 연결되는지 검증한다.

서비스 단위 테스트를 추가한다.

- 테스트 파일은 `src/test/java/com/chukchuk/haksa/domain/admin/service/AdminTestAccountServiceUnitTests.java`다.
- 생성된 user email과 studentCode가 `test_` prefix를 갖는지 검증한다.
- `JwtProvider.createAccessToken`, `JwtProvider.createRefreshToken`, `RefreshTokenService.save` 호출을 검증한다.
- `User.markPortalConnected`를 통해 테스트 계정이 기존 학생 조회 API에서 연결된 계정처럼 동작하도록 한다.

### 3. 옵션 조회와 강의 후보 검색

컨트롤러 테스트를 먼저 추가한다.

- `GET /api/admin/test-options`가 학과 목록과 `FacultyDivision` 영역 목록을 반환하는지 검증한다.
- `GET /api/admin/course-offerings`가 검색 조건을 서비스에 전달하고 강의 후보 목록을 반환하는지 검증한다.

서비스 단위 테스트를 추가한다.

- `AdminTestOptionService`는 `DepartmentRepository.findAll` 결과와 `FacultyDivision.values()`를 응답 DTO로 변환한다.
- `AdminTestOptionService`는 `CourseOfferingRepository` 검색 결과를 `offeringId`, `courseCode`, `courseName`, `year`, `semester`, `credits`, `area`, `departmentName`으로 변환한다.
- 필요한 repository query는 `CourseOfferingRepository`에 명시 JPQL로 추가한다.

### 4. 현재 인증 계정 강의 데이터 수정

컨트롤러 테스트를 먼저 추가한다.

- `PATCH /api/admin/me/graduation-courses`가 principal userId와 요청 바디를 서비스에 전달하는지 검증한다.
- 추가와 삭제를 한 요청에서 함께 받을 수 있게 한다.

서비스 단위 테스트를 추가한다.

- userId로 studentId를 찾고 없으면 기존 `USER_NOT_CONNECTED` 흐름을 따른다.
- 추가 대상 `offeringId`가 있으면 `StudentCourse`를 생성해 저장한다.
- 삭제 대상 studentCourse id가 있으면 현재 학생 소유 row만 삭제한다.
- 수정 후 `AcademicCache.deleteAllByStudentId`를 호출한다.

### 5. 현재 인증 계정 전공 상태 수정

컨트롤러 테스트를 먼저 추가한다.

- `PATCH /api/admin/me/major`가 principal userId와 주전공/복수전공 학과 ID를 서비스에 전달하는지 검증한다.

서비스 단위 테스트를 추가한다.

- 주전공 학과 ID를 `major`에 반영한다.
- 복수전공을 끄면 `secondaryMajor`를 null로 만든다.
- 복수전공을 켜면 복수전공 학과 ID를 `secondaryMajor`에 반영한다.
- 수정 후 `AcademicCache.deleteAllByStudentId`를 호출한다.
- 현재 `Student`에 전공 변경 메서드가 없으므로 도메인 메서드를 최소 범위로 추가한다.

### 6. dev 전용 제한

- `AdminTestController` 또는 admin config에 `@Profile("dev")`를 적용한다.
- `POST /api/admin/test-users`는 토큰 발급 전 진입점이므로 security public endpoint에 추가한다.
- 다른 `/api/admin/me/**` API는 기존 bearer 인증을 사용한다.
- `@ActiveProfiles("dev")` 기반 컨트롤러 테스트로 dev profile에서 로드되는지 검증한다.

### 7. 학과 공개 검색 추가

컨트롤러 테스트를 먼저 추가한다.

- `GET /api/admin/departments?keyword=컴퓨터`가 학과 선택지 목록을 반환하는지 검증한다.
- 토큰 없이 호출 가능한 공개 조회 API로 둔다.

서비스 단위 테스트를 추가한다.

- `AdminTestOptionService`는 keyword가 비어 있으면 전체 학과를 반환한다.
- keyword가 있으면 학과 코드와 학과명에 대해 부분 검색한다.
- `DepartmentRepository`에는 검색용 JPQL을 추가한다.

### 8. 현재 인증 계정 초기화

컨트롤러 테스트를 먼저 추가한다.

- `POST /api/admin/me/reset`이 principal userId를 서비스에 전달하고 성공 메시지를 반환하는지 검증한다.

서비스 단위 테스트를 추가한다.

- 현재 인증 계정의 학생을 찾는다.
- 해당 학생의 `StudentCourse`를 모두 삭제한다.
- 주전공은 소속 학과로 되돌리고 복수전공은 null로 만든다.
- 수정 후 `AcademicCache.deleteAllByStudentId`를 호출한다.

### 9. 현재 인증 계정 테스트 강의 생성

컨트롤러 테스트를 먼저 추가한다.

- `POST /api/admin/me/test-courses`가 테스트 강의 생성 요청과 principal userId를 서비스에 전달하는지 검증한다.
- 응답에는 생성된 `courseCode`, `courseName`, `offeringId`, `studentCourseId`를 포함한다.

서비스 단위 테스트를 추가한다.

- `Course`, `Professor`, `CourseOffering`, `StudentCourse`를 생성하고 현재 학생에게 붙인다.
- `courseCode`가 비어 있으면 `test_` prefix 코드를 생성한다.
- `departmentId`가 있으면 학과를 조회하고, 없으면 `hostDepartment`만 사용한다.
- 수정 후 `AcademicCache.deleteAllByStudentId`를 호출한다.

### 10. OpenAPI 갱신

- `src/main/resources/public/openapi.yaml`에 추가 경로와 요청/응답 스키마를 반영한다.
- API 설명에는 dev 전용 테스트 API임을 명시한다.

## 검증 명령

작업 중에는 작은 테스트부터 실행한다.

```bash
./gradlew test --tests '*AdminTest*'
```

마지막에는 전체 테스트를 실행한다.

```bash
./gradlew test
```

## 커밋 계획

테스트와 구현이 모두 통과하면 한 커밋으로 묶는다.

```bash
git add docs/specs/20260623-dev-test-admin-api src/main/java src/test/java src/main/resources/public/openapi.yaml
git commit -m "279 feat: dev 테스트 어드민 API 추가"
```
