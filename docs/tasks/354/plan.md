# 관리자 문의 API 구현 계획

## 1. DB와 모델

- V17에 `answered_by_admin_id` FK와 최신순·상태·학번 인덱스를 추가한다.
- Report에 답변 관리자 연관을 읽기 전용으로 매핑한다.
- Flyway fresh/upgrade, FK와 인덱스 순서를 검증한다.

## 2. 조회 API

- 정확 일치 검색 타입과 관리자 목록·상세 DTO를 추가한다.
- Specification predicate와 결정적 정렬을 적용한다.
- 목록 최소 정보와 상세 실제 스냅샷·답변자 정보를 매핑한다.

## 3. 답변 API

- 인증 principal의 adminAccountId만 사용한다.
- PENDING 조건부 update로 최초 작성자 승리를 구현한다.
- 404와 중복·경쟁 409를 구분하고 기존 사용자 상세 회귀를 검증한다.

## 4. 검증

- 목록 정렬·페이지·필터·정확 검색·민감 필드 비노출 테스트를 추가한다.
- 상세 nullable 스냅샷과 답변자 정보, 정상·중복·동시 답변을 테스트한다.
- Springdoc과 실행 중 `/v3/api-docs`를 검증한다.
- `./gradlew check --stacktrace --no-daemon`과 `git diff --check`를 통과한다.
- `354 feat: 관리자 문의 관리 API를 구현`으로 커밋한다.

## Wiki

관리자 문의 API, DB 스키마, 상태 전이와 개인정보 취급 Wiki 갱신이 필요하다. 저장소 구현 완료 결과에 별도 Wiki 갱신 필요를 기록한다.

