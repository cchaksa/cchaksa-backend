# 관리자 로컬 인증·인가 및 세션 피벗 계획

## 1. 계약과 migration

- FE에 `/csrf`, `signin`, `me`, `signout`, `password`와 공통 오류 shape를 전달하고 차단 여부를 확인한다.
- V16 다음의 V17 additive migration으로 `login_id/password_hash`를 추가하고 legacy Kakao 컬럼을 nullable로 완화한다.
- fresh/upgrade, unique, nullable과 credential pair 제약을 migration 테스트로 검증한다.

## 2. 인증 구현

- `AdminAccount`와 repository를 case-sensitive exact local credential 기준으로 변경하고 BCrypt strength 12 bean을 추가한다.
- Kakao challenge/token/OIDC 서비스, DTO, controller endpoint와 관리자 Kakao 설정을 제거한다.
- `/csrf` bootstrap, password signin, session cookie, me/signout을 구현한다.
- 비밀번호 변경 시 account write lock, hash 변경, 기존 세션 전체 폐기와 현재 세션 회전을 한 트랜잭션으로 구현한다.

## 3. 보안과 배포 경계

- 미등록·불일치·비활성 계정에 401 `A13`을 통일하고 미등록 계정에도 dummy BCrypt 검증을 수행한다.
- password/hash가 로그, 응답과 문서에 포함되지 않는지 점검한다.
- dev Lambda workflow와 애플리케이션 설정에서 관리자 Kakao 환경 변수 요구를 제거한다.
- 일반 사용자 Kakao 인증과 기존 관리자 session/role 경계의 회귀를 검증한다.

## 4. 테스트와 공개 계약

- 정상/미등록/오류/비활성 signin, validation, CSRF, USER token, session 만료·회수 테스트를 갱신한다.
- password 현재값 불일치, 동일값, 세션 전체 무효화와 현재 세션 회전, 동시 변경 직렬화를 검증한다.
- Springdoc에서 Kakao challenge 계약 제거와 새 DTO/endpoint를 검증한다.
- `./gradlew spotlessApply --no-daemon`, `./gradlew check --stacktrace --no-daemon`, 실행 중 `/v3/api-docs`, `git diff --check`를 수행한다.

## 5. 커밋과 스택 통합

- 문서·migration과 인증 구현을 논리 커밋으로 나눈다.
- 기존 #352 커밋을 rewrite하지 않고 `feat/352` 위에 피벗 커밋을 누적한다.
- 후속 #354의 미적용 문의 migration은 V18로 이동하고 merge 방식으로 새 #352를 반영해 기존 커밋 히스토리와 순차 migration 번호를 보존한다.
- Draft PR #358 본문에 최종 계약, 검증, Wiki 갱신 필요와 WAF rate limit 위험을 기록한다.

## Wiki

인증, DB, 보안 운영 절차, 관리자 계정 발급과 비밀번호 재설정 절차의 Wiki 갱신이 필요하다. 별도 Wiki 저장소는 이번 코드 변경에서 수정하지 않고 최종 결과에 후속 필요를 기록한다.
