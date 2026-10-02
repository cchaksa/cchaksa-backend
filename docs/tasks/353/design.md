# 테스트 데이터 API 경계 분리 설계

## 목표와 범위

개발·테스트 데이터 조작 API를 실제 관리자 API 경계인 `/api/admin/**`에서 제거하고 `/api/test/**`로 이동한다. 테스트 지원 Controller와 Service는 `dev`, `test` 프로파일에서만 생성하며 운영에서는 관련 빈과 MVC 경로가 존재하지 않게 한다.

이번 범위는 기존 테스트 API의 경로·패키지·프로파일·Spring Security·Springdoc·테스트 호출부 정리다. 실제 관리자 인증, 관리자 계정, 문의 관리자 API와 과거 기록인 `docs/specs/**` 수정은 제외한다.

## 패키지와 이름

기존 `com.chukchuk.haksa.domain.admin`은 향후 실제 관리자 도메인이 사용할 경계다. 테스트 지원 코드는 `com.chukchuk.haksa.domain.testsupport`로 이동하고 다음 이름을 사용한다.

- `TestDataController`, `TestDataControllerDocs`.
- `TestDataDto`, `UpdateTransferDataRequest`.
- `TestAccountService`, `TestOptionService`, `TestMutationService`, `TestLectureEvaluationService`.

DTO는 빈이 아니므로 프로파일 대상이 아니다. Controller와 네 Service에 `@Profile({"dev", "test"})`를 적용한다.

## API 경로

Controller 기본 경로를 `/api/test`로 바꾸고 기존 하위 경로는 유지한다.

| 기존 경로 | 변경 경로 | 인증 |
| --- | --- | --- |
| `/api/admin/test-users` | `/api/test/users` | 공개, dev/test 전용. |
| `/api/admin/test-options` | `/api/test/options` | 공개, dev/test 전용. |
| `/api/admin/departments` | `/api/test/departments` | 공개, dev/test 전용. |
| `/api/admin/course-offerings` | `/api/test/course-offerings` | 공개, dev/test 전용. |
| `/api/admin/me/**` | `/api/test/me/**` | USER Bearer JWT, dev/test 전용. |
| `/api/admin/test-lecture-evaluations/**` | `/api/test/lecture-evaluations/**` | 공개, dev/test 전용. |

전역 `SecurityConfig`에서는 기존 `/api/admin/...` 공개 matcher를 모두 제거하고 테스트 경로도 등록하지 않는다. `dev`, `test` 프로파일에서만 활성화되는 테스트 지원 전용 보안 체인이 `/api/test/users`, `/api/test/options`, `/api/test/departments`, `/api/test/course-offerings`, `/api/test/lecture-evaluations/**`를 공개한다. `/api/test/me/**`는 같은 체인에서 기존 USER JWT 인증을 유지한다.

운영 프로파일에서는 Controller가 없으므로 `/api/test/**` MVC 경로가 존재하지 않는다. prod 전용 보안 체인은 이 경로를 인증 응답으로 가로채지 않고 핸들러가 없는 Spring MVC 요청으로 전달해 404를 반환한다. 전역 공개 matcher와 실제 처리 빈에는 테스트 경로가 남지 않는다.

## Springdoc

기본 테스트 프로파일에서 생성되는 `/v3/api-docs`는 변경된 `/api/test/**` 경로를 문서화한다. 운영 프로파일 컨텍스트에서는 TestDataController가 없으므로 해당 경로가 문서에 없어야 한다.

## 대안과 선택 이유

1. 경로만 `/api/test`로 변경한다.
   - 변경량은 작지만 운영에서 테스트 Service 빈이 계속 생성되고 실제 `admin` 패키지 경계가 오염된다.
2. Controller에만 프로파일을 유지하고 Service 이름·패키지를 보존한다.
   - 운영 endpoint는 없지만 테스트 지원 의존성이 운영 빈 그래프와 패키징 의미에 남는다.
3. 경로, 패키지와 Controller·Service 프로파일을 함께 분리한다.
   - 호출부 변경은 크지만 실제 관리자 도메인과 테스트 지원 경계가 명확하다.

3안을 선택한다.

## 검증과 복구

- 변경된 모든 HTTP 통합 테스트를 `/api/test/**`로 실행한다.
- prod 프로파일 ApplicationContext에 TestDataController와 네 Service가 없는지 확인한다.
- prod 프로파일 HTTP 요청에서 `/api/test/**`가 404인지 확인한다.
- Security 필터 테스트에서 공개 경로와 `/api/test/me/**` 보호를 검증한다.
- OpenAPI 계약에서 새 경로와 보안 요구를 검증한다.
- 적용 후 문제가 있으면 migration이나 데이터 변경이 없으므로 코드 revert로 복구할 수 있다.
