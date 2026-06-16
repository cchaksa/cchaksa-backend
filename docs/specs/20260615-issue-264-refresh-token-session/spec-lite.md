# Issue 264 Refresh Token Session Spec Lite

## Goal

같은 `userId`로 여러 기기에서 로그인해도 한 기기의 로그인이나 refresh token 갱신이 다른 기기의 refresh token을 덮어쓰지 않게 한다.

## Design

- Refresh token JWT에 서버가 발급한 세션 식별자 `sid` claim을 넣는다.
- `sid`는 로그인 성공 시점마다 `UUID.randomUUID().toString()`으로 생성한다.
- `sid`는 기기 식별자가 아니라 로그인 세션 식별자다.
- Refresh API는 요청 refresh token의 기존 `sid`를 읽어 같은 세션 row만 갱신한다.
- 새로 저장되는 refresh token은 원문이 아니라 서버 비밀키 기반 HMAC hash로 저장한다.
- 기존 원문 저장 row는 한 번의 정상 refresh를 허용한 뒤 hash-only row로 갱신한다.
- 클라이언트 API 요청과 응답 필드는 바꾸지 않는다.

## Persistence

- `refresh_token` 테이블은 `session_id`를 primary key로 사용한다.
- 기존 `user_id`는 세션 소유자 식별을 위한 일반 컬럼으로 유지한다.
- `token_hash` 컬럼을 추가하고, 새 token 저장 시 `token` 원문 컬럼은 `NULL`로 둔다.
- 기존 운영 row와의 호환을 위해 `token` 컬럼은 nullable로 전환하고 즉시 drop하지 않는다.
- 기존 데이터는 `session_id = user_id`로 backfill한다.
- 기존 `sid` 없는 refresh token은 `userId` fallback으로 조회해 한 번 더 사용할 수 있게 한다.
- 기존 `token_hash` 없는 row는 저장된 `token` 원문으로 검증한 뒤, 새 refresh token의 hash만 저장한다.

## Validation

- 로그인 성공마다 서로 다른 `sid`와 `refresh_token` row가 저장되는지 테스트한다.
- Refresh API가 요청 token의 `sid` row만 갱신하는지 테스트한다.
- 기존 `sid` 없는 token이 `userId` fallback으로 재발급되는지 테스트한다.
- Flyway migration 테스트로 `session_id` 컬럼과 primary key 전환을 검증한다.
- Flyway migration 테스트로 `token_hash` 컬럼 추가와 `token` nullable 전환을 검증한다.
- Refresh token 저장 테스트로 원문 대신 hash만 저장하는지 검증한다.
- Hash 저장 row와 legacy 원문 row의 refresh 검증 경로를 각각 테스트한다.
