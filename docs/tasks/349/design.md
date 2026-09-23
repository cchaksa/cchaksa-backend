# 테스트 계정 편입 데이터 설정

- 이슈: [#349](https://github.com/cchaksa/cchaksa-backend/issues/349).
- `PATCH /api/admin/me/transfer-data`는 `dev/test`에서 인증된 테스트 계정의 원천 데이터만 수정한다. 기존 졸업진단 계산은 변경하지 않는다.
- 테스트 계정은 생성 API의 `studentCode=test_{suffix}`, `email=test_{suffix}@cchaksa.dev`가 일치하는 활성 사용자로 확인한다.

## 입력 계약

6개 필드는 모두 선택 사항이다. 생략은 유지, 명시적 `null`은 400, `{}`는 변경 없는 성공이다. `false`, `0`, `[]`는 실제 수정값이다. 초기화는 기존 reset을 사용한다.

| 필드 | 허용 값과 저장 |
| --- | --- |
| `isTransferStudent` | Boolean. 편입 여부와 입학 구분을 함께 변경한다. true는 `2`, false는 `신입`이다. |
| `totalEarnedCredits` | 0 이상의 정수. 누적 성적에 저장하며 다른 성적 값은 유지한다. |
| `cumulativeGpa` | 0~4.5, 소수 둘째 자리까지. DB의 소수 정밀도와 일치한다. |
| `completedSemesters` | 0 이상의 정수. 등록학기 판정을 새로 추가하지 않는다. |
| `languageCertFulfilled` | Boolean. 기존 외국어 인증 저장 서비스를 사용한다. |
| `designatedCourses` | 지정과목 배열 전체 교체. 빈 배열도 수신 완료로 저장한다. |

지정과목 항목은 원천 필드 `orgClsCd`, `subjtCd`, `subjtNm`, `point`, `precpResnCd`, `cretGainYear`, `cretSmrNm`을 사용한다. `orgClsCd`, `subjtCd`는 공백 불가이며 문자열은 최대 255자다. 학점은 0 이상의 정수, 취득 연도는 1~9999다. 나머지 값은 nullable이다. `sno`와 순서는 인증 학생과 배열 순서에서 생성한다.

## 저장과 초기화

- 학생 행의 쓰기 잠금을 먼저 획득하고 수정한다. 지정과목은 기존 `SyncDesignatedCourseService`의 잠금·버전 규칙을 재사용한다.
- 누적 기록이 없을 때는 전달된 값만 채운 기록을 생성한다. 테스트용 기본 성적을 임의 생성하지 않는다.
- 학과·입학연도·전공은 기존 계정 생성/전공 API, 실제 이수 내역은 기존 강의 API를 사용한다.
- 편입 인정과목은 기존 개설강의 ID를 `/me/graduation-courses`에 추가한다. `/me/test-courses`는 과목 코드에 `test_`를 붙이므로 인정학점 코드 생성 용도로 사용하지 않는다.
- reset은 기존 수강 삭제와 전공 복원을 유지한다. 테스트 계정에 한해 편입 여부·입학 구분·이수 학기를 기본값으로 복원하고 누적 성적·외국어 인증·지정과목 및 수신 버전을 초기화한다. 지정과목 reset 시각은 늦은 포털 스냅샷을 차단하기 위해 남긴다.
- 일반 계정의 기존 reset 범위는 유지한다. 신규 PATCH는 일반 계정을 403으로 거부한다.
- PATCH/reset은 학생별 학사 캐시를 무효화한다. DB schema 및 운영 졸업정책 변경은 없다.

## 검증

- 실제 HTTP에서 테스트 계정 생성 → 수강 설정 → PATCH → 기존 졸업진단 GET → reset → 재설정을 확인한다.
- 부분 갱신, 미설정/0/false/빈 목록, 지정과목 교체와 초기화, 인증·대상 제한, 오류 시 저장 방지, 일반 계정 reset 회귀를 검증한다.
- 실행 서버의 `/v3/api-docs`와 운영 프로필의 라우트 부재를 확인한다.
- Java 17에서 `./gradlew check --stacktrace --no-daemon`, `git diff --check`를 수행한다.
- Wiki `API-and-Authentication`, `Core-Domain-Flows`에 사용법과 reset 범위를 반영한다.

## 검증 결과와 전달 상태

- Java 17에서 `./gradlew spotlessApply check --stacktrace --no-daemon`을 통과했다. 테스트 531건 중 530건 통과, 기존 비활성 `ChukchukHaksaApplicationTests.contextLoads` 1건 건너뜀, 실패·오류 0건이다.
- 실제 임시 HTTP 서버의 인증 계정 생성·PATCH·졸업진단·reset·재설정, OpenAPI, 일반 계정 거부, 원천값 보존, DB 저장 실패 시 트랜잭션 롤백을 확인했다.
- 신규 HTTP 시나리오는 H2 PostgreSQL 호환 모드에서 실행했다. 운영 PostgreSQL 및 배포 환경의 실계정 검증은 수행하지 않았다.
- Checkstyle, Spotless와 `git diff --check`를 통과했다. 첫 전체 검사에서 테스트 코드 스타일 위반을 수정한 뒤 최종 검사를 통과했다.
- Wiki 수정안은 [wiki.patch](wiki.patch)에 보관했다. 별도 Wiki 저장소의 `master`에서 `git apply --check --unidiff-zero <patch 경로>`로 확인 후 적용할 수 있다. 원격 게시와 배포는 하지 않았다.
