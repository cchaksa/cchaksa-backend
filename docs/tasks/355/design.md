# 관리자 문의 검색 쿼리와 인덱스 전략 설계

## 확정 요구사항

- 관리자 검색은 `USER_ID` UUID 전체 일치와 `STUDENT_CODE` 전체 일치만 제공한다.
- prefix와 `%query%` 부분검색, 통합 `OR` 검색은 제공하지 않는다.
- 운영 규모는 사용자 약 4천 명, 월 신규 문의 약 15~20건으로 가정한다.
- PostgreSQL extension은 추가하지 않는다.
- 상태 필터는 선택이며 정렬은 항상 `created_at DESC, id DESC`, 기본 page size는 20이다.
- 검색어, UUID와 학번 원문은 로그, 예외 메시지와 Sentry tag에 기록하지 않는다.

## 대안 비교

### 정확 일치와 B-tree

`user_id = :uuid`와 `student_code = :studentCode`를 각각 실행한다. PostgreSQL B-tree는 equality 조건과 정렬된 결과 반환을 지원하며, 선두 equality 컬럼 뒤에 `created_at DESC, id DESC`를 둔 복합 인덱스는 검색과 최신순 페이지를 함께 처리할 수 있다.

기존 V15의 `idx_reports_user_created_id_desc`와 V17의 `idx_reports_admin_student_code_created_id_desc`, `idx_reports_admin_status_created_id_desc`, `idx_reports_admin_created_id_desc`가 이 계약을 충족한다. 추가 DDL은 필요하지 않다.

### prefix 검색과 B-tree

왼쪽 고정 패턴은 B-tree 후보가 될 수 있지만 전체 일치 요구보다 개인정보 노출 범위가 넓고 선택도가 낮다. CS 업무에서 prefix가 필요하지 않다고 확정했으므로 제공하지 않는다.

### `pg_trgm`과 GIN/GiST

선두 wildcard를 인덱스화할 수 있지만 extension 운영, 추가 인덱스 크기와 쓰기 비용이 발생한다. 부분검색 요구가 없고 데이터 증가율도 낮아 이득이 없다.

### 정규화 검색 컬럼 또는 full-text search

자연어와 여러 텍스트 필드 검색에는 적합할 수 있지만 UUID와 학번 식별자의 전체 일치에는 맞지 않는다. 검색 원문을 복제하는 컬럼과 동기화 책임도 추가되므로 제외한다.

### 필드별 scan과 `UNION ALL`

통합 검색에서 필드별 인덱스를 강제하기 위한 대안이지만 현재 API는 검색 필드를 명시한다. 중복 제거, count와 바깥 정렬 복잡도만 늘어나므로 제외한다.

## 채택안

`searchType=USER_ID|STUDENT_CODE`와 `query`를 함께 받는 기존 정확 일치 계약을 유지한다. USER_ID는 UUID 파싱에 성공해야 하며 STUDENT_CODE는 trim 후 빈 값이 아니고 최대 255자여야 한다. 목록 응답은 본문, 답변과 전체 학적 스냅샷을 포함하지 않는다.

V17은 이미 적용 후보인 선행 migration이므로 수정하지 않는다. 선택안에 필요한 인덱스가 모두 존재해 V18도 추가하지 않는다. 향후 부분검색 요구가 새로 생기면 별도 이슈에서 데이터 규모와 실제 검색 패턴을 다시 측정한다.

## 성능 검증

Embedded PostgreSQL에 사용자 4천 명과 문의 2만 건을 생성한다. 상태 비율은 실행 계획의 선택도 검증을 위해 PENDING 10%, ANSWERED 90%로 가정하고 `ANALYZE` 뒤 다음을 `EXPLAIN (ANALYZE, BUFFERS)`로 확인한다. 이 비율은 운영 측정값이 아니라 테스트 가정이다.

- 전체·상태별 최신 20건.
- USER_ID·STUDENT_CODE 전체 일치와 상태 결합.
- 각 정확 검색의 count query.
- 큰 offset에서 건너뛴 행도 처리되는 특성.
- `LIKE '%query%'`에는 선택적인 B-tree `Index Cond`가 생성되지 않는 특성.

실행 시간은 CI 장비와 캐시에 민감하므로 자동 실패 임계치로 사용하지 않는다. 인덱스 이름, `Index Cond`, sort 범위, buffers와 실제 처리 행을 검증한다. 전체·상태 최신순은 인덱스 순서로 별도 Sort 없이 처리되는지 확인한다. 사용자별 문의처럼 결과가 매우 적으면 planner가 bitmap scan 뒤 일치 행만 정렬할 수 있으며, 정렬 입력이 정확 일치 결과로 제한되면 허용한다. 운영에서 page 50 이상, 즉 offset 1,000 이상의 접근이 반복되면 cursor pagination 전환 검토를 시작하고 실제 P95와 실행 계획을 근거로 별도 이슈를 연다.

## 근거와 운영

- PostgreSQL 공식 문서상 B-tree는 equality와 정렬 결과를 지원하며 선두 wildcard는 B-tree 검색 조건으로 사용할 수 없다.
- 큰 offset의 행도 서버에서 계산되므로 페이지가 깊어질수록 비효율적이다.
- 배포 뒤 `pg_stat_user_indexes`의 `idx_scan`, `idx_tup_read`, `idx_tup_fetch`와 느린 쿼리의 실행 계획을 확인한다. 검색 값 원문은 모니터링 tag나 로그에 넣지 않는다.

참고 문서:

- https://www.postgresql.org/docs/current/indexes-types.html
- https://www.postgresql.org/docs/current/indexes-multicolumn.html
- https://www.postgresql.org/docs/current/indexes-examine.html
- https://www.postgresql.org/docs/current/queries-limit.html
- https://www.postgresql.org/docs/current/using-explain.html

## 복구

신규 migration과 extension이 없으므로 #355 변경은 API validation annotation, 테스트와 문서만 revert할 수 있다. V17 인덱스의 제거는 이 이슈의 복구 절차에 포함하지 않는다.
