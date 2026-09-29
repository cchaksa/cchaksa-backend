# 관리자 문의 검색 쿼리와 인덱스 전략 구현 계획

## 1. 계약 고정

- `USER_ID`, `STUDENT_CODE` 전체 일치만 유지한다.
- 검색어 최대 255자를 Controller validation과 Springdoc에 명시한다.
- 검색 타입·검색어 동시 입력, UUID 형식, 빈 값과 초과 길이 실패 계약을 검증한다.
- 부분검색, prefix, extension과 통합 `OR`는 구현하지 않는다.

## 2. PostgreSQL 실행 계획 검증

- `AdminReportIndexPostgresTest`의 stub에 V15 사용자 인덱스와 `user_id`를 추가한다.
- 사용자 4천 명, 문의 2만 건과 PENDING 10%의 결정적 테스트 데이터셋을 생성하고 `ANALYZE`한다.
- 전체·상태 목록이 목적 인덱스 순서로 별도 Sort 없이 처리되는지 확인한다.
- USER_ID·STUDENT_CODE 정확 검색과 상태 결합은 목적 인덱스와 제한된 일치 행을 사용하는지 확인한다. 소수 일치 행의 bounded Sort는 허용한다.
- USER_ID·STUDENT_CODE count query의 인덱스 사용을 확인한다.
- `EXPLAIN (ANALYZE, BUFFERS)`에 실행·buffer 정보가 존재하는지 확인한다.
- 큰 offset의 처리 행 증가와 선두 wildcard에 선택적 `Index Cond`가 없는 조건을 재현한다.

## 3. API·회귀 테스트

- 정확 일치와 부분검색 불허 테스트를 유지한다.
- 누락·공백·잘못된 UUID·255자 초과 검색어가 400인지 검증한다.
- 목록 민감 필드 비노출, 상태 필터, tie-breaker와 페이지 계약을 회귀 검증한다.
- 실행 중 `/v3/api-docs`에서 검색어 최대 길이와 허용 enum을 확인한다.

## 4. 검증과 커밋

- `./gradlew spotlessApply --no-daemon`.
- 관련 관리자 문의·PostgreSQL·OpenAPI 테스트를 실행한다.
- `./gradlew check --stacktrace --no-daemon`.
- `git diff --check`와 민감 정보 문자열을 검토한다.
- `355 test: 관리자 문의 정확 검색 전략을 검증`으로 커밋한다.

## Wiki

관리자 문의 검색 계약, B-tree 선택 근거, offset 모니터링과 인덱스 사용 관측 방법을 관련 Wiki에 갱신해야 한다. 저장소 커밋에는 Wiki 저장소 변경을 포함하지 않고 최종 결과에 필요 여부를 기록한다.
