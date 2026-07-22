# 이메일 없는 소셜 사용자 로그인 허용 설계

## 연결 이슈

- GitHub Issue [#314](https://github.com/cchaksa/cchaksa-backend/issues/314)
- Sentry `JAVA-SPRING-BOOT-4Z`, `JAVA-SPRING-BOOT-4P`

## 확인된 원인

최신 두 Sentry 이벤트는 같은 `POST /api/users/signin` trace에서 발생했다. OIDC 검증 후 `UserService.findOrCreateUser`가 이메일이 없는 신규 사용자를 생성했고, `users.email=NULL` INSERT가 DB NOT NULL 제약을 위반했다. 같은 신규 가입 흐름에서 생성하는 `social_accounts.email`도 현재 NOT NULL이다.

## 처리 정책

- 이메일 claim이 없는 신규 소셜 사용자도 가입과 로그인을 허용한다.
- `users.email`과 `social_accounts.email`은 실제 부재 의미를 보존해 `NULL`로 저장한다.
- 빈 문자열이나 임의 이메일을 만들지 않는다.
- 기존 SocialAccount가 있으면 새 사용자를 만들지 않고 기존 사용자로 로그인한다.
- access token은 이메일이 있으면 기존처럼 claim을 포함하고, 없으면 email claim 없이 발급할 수 있어야 한다.

## 변경 범위

- 새 Flyway migration으로 두 이메일 컬럼의 NOT NULL을 제거한다.
- `User`와 `SocialAccount` 엔티티의 nullable 메타데이터를 DB와 맞춘다.
- 이메일 유무별 신규 가입, 이메일 없는 기존 계정 로그인, nullable 저장과 JWT 발급을 테스트한다.

로그인 요청·응답 DTO에는 이메일 필드가 없으므로 공개 API 응답 계약과 OpenAPI 문서는 변경하지 않는다.

## Migration 순서 주의

현재 `origin/main`의 마지막 migration은 V8이지만, 아직 병합되지 않은 PR #315가 V9를 사용한다. 이 브랜치는 현재 main 기준 규칙에 따라 V9를 추가하되 Draft로 유지한다. PR #315가 먼저 병합되면 이 브랜치를 최신 main에 rebase하고 migration을 다음 version으로 변경한 뒤 전체 테스트를 다시 실행해야 한다.

## 검증

- 이메일 있는 신규 사용자의 기존 가입 동작이 유지된다.
- 이메일 없는 신규 User와 SocialAccount가 NULL 이메일로 저장된다.
- 이메일 claim이 없는 기존 SocialAccount 로그인이 신규 생성을 시도하지 않는다.
- null 이메일 사용자도 access token과 refresh token을 발급받는다.
- Flyway 전체 migration과 `./gradlew test --stacktrace --no-daemon`이 통과한다.

## 운영 확인

배포 후 `JAVA-SPRING-BOOT-4Z`, `JAVA-SPRING-BOOT-4P` 재발 여부를 확인한 뒤 Sentry resolve 여부를 결정한다.
