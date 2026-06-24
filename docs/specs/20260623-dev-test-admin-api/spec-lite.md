# dev 전용 프론트 테스트 어드민 API Spec Lite

## 목적

프론트엔드가 dev 환경에서 테스트 계정을 즉시 만들고, 해당 토큰 계정의 졸업요건 강의 데이터와 전공 상태를 API로 조작할 수 있게 한다.

## 범위

- `feat/279` 이슈 범위의 dev 전용 API를 추가한다.
- 테스트 계정 생성 API는 새 사용자와 학생 데이터를 만들고 `accessToken`, `refreshToken`을 반환한다.
- 조회 API는 프론트가 수정 요청에 사용할 학과, 졸업요건 영역, 강의 후보를 내려준다.
- 수정 API는 요청 토큰의 계정에 연결된 학생 데이터만 변경한다.
- 운영 profile에서는 어드민 컨트롤러가 로드되지 않아야 한다.

## API 기본안

- `POST /api/admin/test-users`.
- `GET /api/admin/test-options`.
- `GET /api/admin/course-offerings`.
- `PATCH /api/admin/me/graduation-courses`.
- `PATCH /api/admin/me/major`.
- `GET /api/admin/departments`.
- `POST /api/admin/me/reset`.
- `POST /api/admin/me/test-courses`.

## 결정 사항

- 테스트 계정의 `email`과 `studentCode`에는 `test_` prefix를 붙인다.
- 강의 데이터 수정은 테스트 계정만 대상으로 제한하지 않는다.
- 강의 데이터와 전공 상태 수정은 `Authorization` 토큰의 userId에 연결된 학생을 대상으로 한다.
- 테스트 계정 삭제 또는 만료 정책은 만들지 않는다.
- 강의 추가는 1차 구현에서 기존 `CourseOffering`의 `offeringId` 기반으로 처리한다.
- 임의 테스트 강의 생성은 전역 강의 마스터 관리 API가 아니라 현재 인증 계정에 테스트 강의를 바로 추가하는 제한된 API로 제공한다.
- 테스트 계정 초기화는 현재 인증 계정의 학생 수강 데이터를 비우고 주전공을 소속 학과로, 복수전공을 없음으로 되돌린다.
- 학과 조회는 토큰 없이 `keyword`로 학과 코드와 학과명을 검색할 수 있게 한다.

## 완료 조건

- dev 환경에서 테스트 계정을 생성하면 토큰 두 개가 내려온다.
- dev 환경에서 학과, 졸업요건 영역, 강의 후보를 조회할 수 있다.
- dev 환경에서 토큰 없이 학과를 검색할 수 있다.
- 인증 토큰 계정의 강의 데이터를 추가하고 삭제할 수 있다.
- 인증 토큰 계정의 테스트 강의를 생성해 바로 추가할 수 있다.
- 인증 토큰 계정의 학생 테스트 데이터를 초기화할 수 있다.
- 인증 토큰 계정의 주전공과 복수전공 상태를 변경할 수 있다.
- 관련 응답이 `src/main/resources/public/openapi.yaml`에 반영된다.
- 관련 단위 테스트, 컨트롤러 테스트, 전체 `./gradlew test`가 통과한다.

## 제외

- prod 환경 활성화.
- 별도 어드민 권한 체계.
- 테스트 계정 삭제 API.
