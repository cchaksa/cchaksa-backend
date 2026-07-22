# 스크래핑 area code 자동 등록 설계

## 연결 이슈

- GitHub Issue [#311](https://github.com/cchaksa/cchaksa-backend/issues/311)

## 목적

포털의 유효한 선교 영역 코드가 `liberal_arts_area_codes`에 없더라도 `course_offerings.area_code` FK 오류 없이 멱등적으로 저장한다.

## 확인된 원인

- Sentry `JAVA-SPRING-BOOT-4V`, `JAVA-SPRING-BOOT-4T`의 최신 이벤트는 `area_code=8` 누락으로 발생했다.
- 원본 S3 payload는 `cltTerrNm=8영역`, `cltTerrCd=28`을 포함한다.
- 현재 매퍼는 첫 문자만 읽으므로 `10영역`을 1로 잘못 변환할 수 있다.
- 현재 서비스는 `getReferenceById`만 호출해 기준정보 누락을 INSERT flush 전에는 발견하지 못한다.

## 결정

- `숫자+영역` 형식만 유효한 area code로 인정하고 전체 숫자를 파싱한다.
- null, 공백, 형식 오류, 0은 area code 없음으로 처리한다.
- 유효한 non-zero 코드는 PostgreSQL `INSERT ... ON CONFLICT DO NOTHING`으로 기준정보를 먼저 등록한다.
- 자동 생성 행은 `code=<파싱값>`, `area_name=<파싱값>영역`, `is_active=TRUE`로 저장한다.
- 코드 8은 배포 직후에도 존재하도록 `V9` migration에 추가한다.
- `original_area_code`와 S3 원본 payload 보존 방식은 변경하지 않는다.
- 추가 기준정보 행은 이전 Lambda 코드도 그대로 읽을 수 있으므로 migration은 backward compatible하다.
- Sentry 이슈는 배포와 재발 여부 확인 전에는 resolve 하지 않는다.

## 검증

- 정상 코드, 누락 코드, 형식 오류, 두 자리 코드의 매핑을 검증한다.
- 유효 코드의 자동 등록과 null/0 미등록을 검증한다.
- 실제 PostgreSQL에서 동시 등록과 같은 트랜잭션의 FK 저장을 검증한다.
- `V9__add_liberal_arts_area_code_8.sql` 적용 결과를 검증한다.
- `./gradlew test --stacktrace --no-daemon`을 실행한다.
- 독립 리뷰로 동시성, 멱등성, migration, 변경 범위를 확인한다.

## 문서 갱신 판단

- 외부 API, 운영 절차, 공용 아키텍처를 변경하지 않으므로 Wiki는 갱신하지 않는다.
- 이슈별 데이터 해석과 자동 등록 정책은 이 설계 문서와 PR에 남긴다.

## 제외 범위

- 미등록 값을 null이나 공통 코드로 치환하지 않는다.
- 외부 API 응답 구조는 변경하지 않는다.
- 회원 탈퇴 Sentry 이슈 `JAVA-SPRING-BOOT-4W`, `JAVA-SPRING-BOOT-3N`은 다루지 않는다.
