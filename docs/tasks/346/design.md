# 사용자 문의 작성 및 본인 문의 조회 설계

## 목표와 범위

인증 사용자가 제목과 텍스트 본문으로 문의를 등록하고, 본인의 문의 목록과 상세·답변 상태를 조회할 수 있게 한다. 관리자 화면에서 사용할 사용자·학적 정보는 클라이언트 입력이 아니라 문의 생성 시점의 서버 데이터로 스냅샷을 만든다.

이번 범위는 사용자용 `POST /api/reports`, `GET /api/reports`, `GET /api/reports/{reportId}`다. 관리자 목록·답변 API, 알림, 첨부파일, 문의 수정·삭제, 답변 이력은 제외한다.

## API 결정

모든 endpoint는 `/api/reports`를 사용하며 Bearer JWT 인증을 요구한다. 인증 주체가 곧 현재 문의 소유자이므로 `/api/users/me/reports`를 별도로 만들지 않는다. 향후 관리자 API는 `/api/admin/reports`로 분리한다.

| Method | Path | 계약 |
| --- | --- | --- |
| `POST` | `/api/reports` | `title`, `content`만 받아 201과 `Location`을 반환한다. |
| `GET` | `/api/reports?page=0&size=20` | 본인 문의를 `createdAt DESC, id DESC`로 페이지 조회한다. |
| `GET` | `/api/reports/{reportId}` | 본인 문의의 제목·본문과 nullable 답변을 반환한다. |

존재하지 않는 문의는 문의 전용 404 오류를 반환한다. 존재하지만 다른 사용자 소유이면 기존 `C04 FORBIDDEN`과 403을 반환한다. 403 응답에는 문의 제목·상태·작성자 등 리소스 정보를 포함하지 않는다.

목록은 offset pagination을 사용한다. 동일 시각 행은 `id DESC`로 결정적으로 정렬하지만 페이지 조회 사이 새 문의가 추가되면 뒤 페이지가 이동할 수 있는 특성은 이번 범위에서 허용한다.

## 저장 모델

`reports`는 문의와 현재 소유자, 생성 당시 제출자·학적 스냅샷을 함께 저장한다.

### 문의와 소유권

- `id`: UUID PK다.
- `user_id`: 현재 문의 소유자이며 `users.id` FK다.
- `submitted_user_id`: 문의 생성 당시 사용자 ID 스냅샷이다. 계정 병합 뒤에도 바뀌지 않으며 회원 탈퇴 익명화 시 null이 된다. FK는 두지 않는다.
- `title`: trim 후 1~100자의 plain text다.
- `content`: trim 후 1~5,000자의 plain text다.
- `status`: `PENDING`, `ANSWERED`다.
- `answer`, `answered_at`: 답변 전에는 모두 null이고 답변 완료 상태에서는 모두 존재한다.
- `created_at`, `updated_at`: JPA auditing이 채우며 DB에서도 NOT NULL이다.

### 사용자·학적 스냅샷

- 학과 ID·이름.
- 학번.
- 주전공 ID·이름.
- 복수전공 ID·이름.
- 편입 여부.
- 입학 연도.
- 졸업요건 상태.

학생 정보가 없어도 문의를 생성한다. 이때 학적 필드는 null이고 졸업요건 상태는 `UNKNOWN`이다. 사용자 학적이 나중에 바뀌어도 기존 스냅샷은 갱신하지 않는다.

### 졸업요건 상태

학번 문자열에서 입학 연도를 추측하지 않는다. 학생과 저장된 입학 연도가 있으면 기존 `GraduationMajorResolver`를 호출해 주전공 우선·학과 fallback·학과 별칭·복수전공 조합 정책을 그대로 사용한다.

| 조건 | 저장 상태 |
| --- | --- |
| 학생 또는 입학 연도가 없음 | `UNKNOWN` |
| resolver 성공 | `AVAILABLE` |
| resolver가 `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND(G02)` 반환 | `NOT_AVAILABLE` |
| 그 밖의 예외 | 문의 생성을 실패시키고 예외 전파 |

## DB 불변식과 인덱스

신규 Flyway migration은 additive하게 빈 `reports` 테이블·제약·인덱스만 추가하므로 이전 Lambda 코드가 새 schema에서도 동작한다.

- `status IN ('PENDING', 'ANSWERED')`다.
- `graduation_requirement_status IN ('AVAILABLE', 'NOT_AVAILABLE', 'UNKNOWN')`다.
- `PENDING`이면 `answer IS NULL AND answered_at IS NULL`이다.
- `ANSWERED`이면 `answer IS NOT NULL`, `BTRIM(answer) <> ''`, `answered_at IS NOT NULL`이다.
- `user_id` FK는 cascade delete를 사용하지 않는다.
- 사용자별 최신순 조회를 위해 `(user_id, created_at DESC, id DESC)` B-tree 인덱스를 둔다.

문제 발생 시 적용된 migration을 수정하거나 자동 rollback하지 않는다. 보정은 다음 version의 forward migration으로 수행한다.

## 계정 수명주기

### 계정 병합

`UserService.tryMergeWithExistingUser`는 기존 사용자를 물리 삭제한다. 삭제 전에 기존 사용자 소유 문의의 `user_id`를 새 사용자로 일괄 이전한다. `submitted_user_id`와 학적 스냅샷은 변경하지 않는다. 병합 뒤 새 사용자가 과거 문의를 계속 조회한다.

### 회원 탈퇴

회원 탈퇴 트랜잭션에서 문의 제목·본문·답변은 CS 기록으로 유지한다. 다음 구조화된 식별 스냅샷을 제거한다.

- `submitted_user_id`.
- 학번.
- 학과·주전공·복수전공 ID와 이름.
- 편입 여부.
- 입학 연도.

졸업요건 상태는 `UNKNOWN`으로 바꾼다. `user_id`는 soft-deleted 사용자 행을 계속 참조한다. 사용자는 탈퇴 뒤 인증할 수 없으므로 사용자 API에서 접근할 수 없다. CS 기록의 최종 보존 기간과 운영 삭제 정책은 후속 관리자 기능에서 정한다.

## 대안과 선택 이유

1. 학적을 조회 시점에 현재 사용자 데이터와 join한다.
   - 저장 중복은 적지만 문의 당시 컨텍스트가 사라지고 탈퇴·학과 변경으로 관리자 화면이 달라진다.
2. 모든 사용자 정보를 클라이언트 요청으로 받는다.
   - 구현은 단순하지만 위변조된 학번·학과·졸업요건 상태를 저장할 수 있다.
3. 서버가 생성 시점 스냅샷을 저장한다.
   - 저장 공간과 개인정보 수명주기 처리가 필요하지만 문의 당시 컨텍스트가 안정적이고 클라이언트 위변조를 막는다.

3안을 선택한다. 소유권용 `user_id`와 생성 당시 `submitted_user_id`를 분리해 계정 병합과 역사 보존을 동시에 처리한다.

## 보안과 로깅

- 요청 Body는 제목과 본문만 허용한다.
- 학번·문의 본문·답변은 업무 로그, 예외 메시지, Sentry tag에 기록하지 않는다.
- 목록 응답에는 본문·답변·학적 스냅샷을 포함하지 않는다.
- 사용자 상세 응답에도 관리자용 스냅샷을 포함하지 않는다.
- 다른 사용자 문의의 403 응답에는 해당 문의 정보를 포함하지 않는다.

## 완료 조건

1. 작성·목록·상세 API와 Springdoc 계약이 구현된다.
2. 스냅샷 3상태와 계정 병합·회원 탈퇴 정책이 테스트된다.
3. migration의 fresh/V14 업그레이드, FK, CHECK, 인덱스 순서·방향을 검증한다.
4. 실행 중인 `/v3/api-docs`, 관련 API, 전체 Gradle check를 검증한다.
5. Wiki의 API, 핵심 흐름, 아키텍처 문서를 갱신한다.
