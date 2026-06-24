# dev 전용 프론트 테스트 어드민 API Context Notes

## 2026-06-23

- 루트 checkout은 `dev...origin/dev [behind 108]`이고 사용자 변경으로 보이는 파일이 있었다.
- 사용자 변경을 보호하기 위해 `/private/tmp/haksa-feat-279`에 `origin/dev` 기준 격리 worktree를 만들었다.
- 기준선 `./gradlew test`는 성공했다.
- 기존 공개 API에는 학과 목록, 졸업요건 영역 목록, 강의 후보 검색 API가 없다.
- 기존 `GET /api/graduation/progress`는 현재 계정의 졸업 진행도와 영역별 이수 과목을 보여주지만 수정용 선택지 전체를 제공하지 않는다.
- `Student`는 `department`, `major`, `secondaryMajor`를 `Department`로 참조한다.
- `StudentCourse`는 `CourseOffering`을 필수로 참조하므로 강의 추가는 기존 `offeringId` 기반이 가장 작고 안전하다.
- 임의 테스트 강의 생성은 `Course`, `CourseOffering`, `StudentCourse`를 함께 만들 가능성이 있어 이번 1차 구현의 기본 경로에서 제외한다.
- 학생 단위 캐시는 `AcademicCache.deleteAllByStudentId(UUID studentId)`로 무효화할 수 있다.
- 테스트 계정 생성 토큰은 기존 `JwtProvider`와 `RefreshTokenService`를 재사용한다.
- dev 전용 제한은 `@Profile({"dev", "test"})` 기반으로 둔다.
- `test` profile은 컨트롤러 slice 테스트 로딩용이며, 실제 dev 서버에서는 `dev` profile로만 사용한다.
- `POST /api/admin/test-users`는 토큰 발급 전 진입점이므로 SecurityConfig의 public endpoint에 추가했다.
- `/api/admin/me/**` API는 기존 bearer 인증 principal의 userId로 현재 계정 학생을 찾는다.
- 강의 추가는 `CourseOffering`의 영역과 요청 영역이 다르면 `INVALID_ARGUMENT`로 막는다.
- `./gradlew test --tests '*AdminTest*'`와 `./gradlew test`는 모두 통과했다.

## 2026-06-24

- `Student.isTransferStudent()`는 학번 앞 2자리와 입학년도 뒤 2자리를 비교해 편입생 여부를 계산한다.
- 테스트 계정 학번은 `test_` prefix를 사용하므로 기존 비교식에서는 `te`와 입학년도 suffix가 달라 편입생으로 오판된다.
- 테스트 계정은 프론트 개발용 데이터이며 편입생 미지원 졸업요건 흐름을 막으면 안 되므로 `test_` 학번은 편입생 판정에서 제외한다.
- 테스트 옵션 조회와 강의 후보 조회는 프론트가 토큰 발급 전 선택지를 구성하는 데 필요하므로 dev 공개 조회 API로 둔다.
- 데이터 변경 API인 `/api/admin/me/graduation-courses`, `/api/admin/me/major`는 계속 인증 계정 기준으로 동작하므로 Bearer token을 요구한다.
- 포털 동기화 경로의 `CreateOfferingCommand.departmentId`는 `null`이고 `hostDepartment` 문자열이 남을 수 있으므로, 강의 후보 학과 필터는 학과 ID를 학과명으로 해석한 뒤 `CourseOffering.department.establishedDepartmentName`과 `CourseOffering.hostDepartment`를 함께 비교한다.
- 선교처럼 학과 필터가 애매한 영역은 `departmentId`를 생략해 검색한다.
