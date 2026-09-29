# 관리자 문의 목록·상세·답변 API 설계

## API 계약

- `GET /api/admin/reports?page=0&size=20&status=PENDING&searchType=USER_ID&query={uuid}`.
- `GET /api/admin/reports/{reportId}`.
- `POST /api/admin/reports/{reportId}/answer` body `{ "answer": "..." }`.

모든 경로는 관리자 세션과 `ROLE_ADMIN` 또는 `ROLE_CS_AGENT`를 요구한다. reportId와 userId는 UUID다. 목록은 `createdAt DESC, id DESC`로 정렬하고 본문, 답변, 전체 학적 스냅샷을 반환하지 않는다.

검색은 `USER_ID`, `STUDENT_CODE` 정확 일치만 지원한다. `searchType`과 `query`는 함께 전달해야 한다. 통합 OR 검색과 부분검색은 #355로 보류한다. optional parameter OR JPQL을 피하고 JPA Specification으로 실제 전달된 predicate만 생성한다.

## 목록과 인덱스

목록 항목은 `reportId`, `status`, `title`, 현재 `userId`, 스냅샷 `studentCode`, `createdAt`, `answeredAt`만 반환한다. 기본 size는 20이며 허용 범위는 1~100이다.

V17에 다음 B-tree를 추가한다.

- 전체 최신순: `(created_at DESC, id DESC)`.
- 상태별 최신순: `(status, created_at DESC, id DESC)`.
- 학번별 최신순: `(student_code, created_at DESC, id DESC)`.

기존 `(user_id, created_at DESC, id DESC)`는 USER_ID 검색에 재사용한다.

## 상세와 감사 정보

상세는 제목, 본문, 상태, 현재 userId, 생성·답변 시각과 실제 DB 스냅샷인 submittedUserId, 학번, 학과, 주전공, 복수전공, 편입 여부, 입학 연도, 졸업요건 상태를 반환한다. DB에 없는 category, errorCode, universityName, grade, semester는 추가하지 않는다.

`reports.answered_by_admin_id`는 nullable UUID FK로 추가한다. nullable은 migration 뒤 이전 Lambda가 계속 동작할 수 있게 하고 기존 데이터 호환성을 유지하기 위한 것이며, 신규 관리자 답변 API는 항상 인증 principal의 UUID를 기록한다. 상세의 답변자 표시는 현재 `admin_accounts.display_name`을 사용하고 감사 원본은 UUID다.

## 답변 동시성

답변은 trim 후 1~5,000자 plain text다. 저장은 다음 의미의 단일 SQL로 처리한다.

`UPDATE reports SET ... WHERE id = :id AND status = 'PENDING'`.

갱신 수가 1이면 최초 작성자가 성공한다. 0이면 문의 존재 여부를 확인해 없으면 404 `R01`, 이미 답변됐으면 409 `R02`를 반환한다. 답변 수정·삭제는 제공하지 않는다. 향후 수정과 append-only 감사 이력은 `report_answers` 또는 `report_actions` 테이블을 추가해 확장하며 현재 reports 컬럼을 덮어쓰는 API는 만들지 않는다.

본문, 답변, 학번과 학적 스냅샷은 로그, 예외 메시지, Sentry tag에 기록하지 않는다.

