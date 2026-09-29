# 관리자 인증·인가 및 세션 설계

## 경계

관리자 인증은 일반 사용자 `UserService`, 사용자 JWT principal, refresh token과 별도 흐름으로 구현한다. 관리자 권한의 정본은 `admin_accounts`이며 `users`와 `social_accounts`를 참조하지 않는다. 사용자 Bearer JWT는 관리자 API에서 관리자 권한으로 승격되지 않는다.

카카오 구현은 세 가지 대안을 비교했다.

1. 기존 `KakaoOidcService` 직접 재사용은 사용자 앱 key와 예외·수명주기를 결합하므로 제외한다.
2. 전체 구현 복제는 보안 수정이 두 군데로 갈라지므로 제외한다.
3. JWKS 조회와 카카오 토큰 서명·표준 claim 검증만 중립 컴포넌트로 공유하고, 사용자·관리자 서비스와 audience 설정은 분리한다.

3안을 사용한다. 관리자 Kakao 앱의 JavaScript key, REST API key, client secret과 redirect URI는 일반 사용자 앱 설정과 분리한다. JavaScript key는 브라우저 SDK 초기화와 ID token audience 검증에 사용되는 공개 값이고 challenge 응답으로 전달한다. REST API key와 client secret은 서버의 token 교환에만 사용하며 응답, 로그, 문서와 Git에 실제 값을 남기지 않는다. redirect URI는 서버 설정의 고정값이며 요청 body에서 받지 않는다.

## 로그인 계약

1. `GET /api/admin/auth/challenge`가 UUID `challengeId`, 난수 `nonce`, 난수 `state`, 공개 `javascriptAppKey`, 고정 `redirectUri`를 발급한다. 응답은 `Cache-Control: no-store`이며 SPA의 `/login`에서도 읽을 수 있도록 Path가 `/`인 `XSRF-TOKEN` 쿠키와 challenge를 로그인 시작 브라우저에 결합하는 HttpOnly `cchaksa_admin_login` 쿠키를 생성한다. 결합 쿠키 원문은 브라우저에만 전달하고 DB에는 SHA-256 hash만 저장한다.
2. 프론트는 응답 값을 그대로 `Kakao.Auth.authorize`에 전달한다. Kakao는 고정된 `/login/callback` URI에 authorization code와 state를 전달한다.
3. `POST /api/admin/auth/signin`은 `{challengeId, authorizationCode, state}`와 `X-XSRF-TOKEN`을 받는다. 서버는 DB의 state와 브라우저 결합 쿠키 hash가 모두 일치하는 미사용 challenge를 원자적으로 소모한다.
4. 서버는 authorization code를 Kakao token endpoint에 `grant_type=authorization_code`, 관리자 REST API key, 서버 고정 redirect URI와 설정된 경우 client secret으로 제출한다. 요청 body의 redirect URI는 받거나 신뢰하지 않는다. 외부 호출에는 기본 connect 3초, read 5초 timeout을 적용하고 환경 변수로 조정할 수 있다.
5. token 응답의 ID token을 관리자 JavaScript key audience와 저장된 nonce로 검증한다. `provider`와 `socialId`는 요청에서 받지 않으며 검증된 `sub`로 ACTIVE `admin_accounts`를 조회한다.
6. 성공 시 불투명 세션 난수를 `cchaksa_admin_session` Secure, HttpOnly, SameSite=Strict 쿠키로 발급하고 DB에는 SHA-256 해시만 저장한다.
7. `GET /api/admin/auth/me`는 `adminAccountId`, `displayName`, `adminRole`을 반환한다.
8. `POST /api/admin/auth/signout`은 현재 세션을 폐기하고 세션 쿠키와 CSRF 쿠키를 만료한다.

challenge는 짧게 만료되고 한 번의 로그인 시도에만 사용할 수 있다. 세션은 idle timeout과 absolute timeout을 모두 검사하며, 매 요청에서 계정 ACTIVE 상태를 재검증해 권한 회수를 즉시 반영한다.

## 모델

- `admin_accounts`: UUID PK, provider(KAKAO), social_id unique, display_name, admin_role(ADMIN/CS_AGENT), status(ACTIVE/INACTIVE), created_by, last_login_at, created_at, updated_at.
- `admin_login_challenges`: UUID PK, nonce, state, browser_token_hash, expires_at, used_at, created_at. nonce, state와 브라우저 결합 token은 짧은 기간 뒤 삭제 가능한 일회성 값이며 token 원문은 저장하지 않는다.
- `admin_sessions`: UUID PK, admin_account_id FK, token_hash unique, expires_at, idle_expires_at, last_accessed_at, revoked_at, created_at.

## 보안 체인

`/api/admin/**` 전용 우선순위 SecurityFilterChain을 둔다. challenge와 signin만 익명 접근을 허용하고 모든 상태 변경에 Cookie CSRF를 적용한다. OAuth state는 Kakao redirect를 challenge에 결합하고, HttpOnly 일회성 쿠키는 challenge를 로그인 시작 브라우저에 결합하며, Cookie CSRF는 signin POST를 보호하므로 세 값을 모두 검증한다. 관리자 세션 필터는 `AdminPrincipal`에 `ROLE_ADMIN` 또는 `ROLE_CS_AGENT`를 부여한다. 일반 JWT 필터도 관리자 체인에서 실행하되 일반 principal에는 관리자 authority가 없으므로 USER token은 403이 된다. 미인증은 401이다.

Host, Origin, CORS는 권한 판단에 사용하지 않는다. CORS는 브라우저 전송 경계일 뿐이며 최종 인가는 DB 세션과 authority가 담당한다.

## 운영과 호환성

V16은 신규 테이블만 추가하므로 이전 Lambda와 호환된다. 아직 배포되지 않은 V16 challenge 테이블에 state를 포함한다. 기본 수명은 challenge 5분, idle 30분, absolute 8시간이며 환경 변수로 조정한다. authorization code, client secret, 세션·nonce·state·ID Token 원문과 socialId는 로그나 Sentry tag에 남기지 않는다.

## 오류 계약

- body validation 실패: 400 `C01`.
- 만료·사용 challenge, state 또는 브라우저 결합 쿠키 불일치: 401 `A10`.
- Kakao token 교환 실패 또는 ID token 부재: 401 `A12`.
- ID token 서명·audience·nonce·만료 검증 실패: 401 `T01`~`T10`.
- 미등록·비활성 관리자: 403 `A09`.
- CSRF 불일치·누락: 403.

## 공식 근거

- Kakao JavaScript SDK `authorize`: https://developers.kakao.com/sdk/reference/js/release/Kakao.Auth.html
- Kakao Login JavaScript 흐름: https://developers.kakao.com/docs/en/kakaologin/js
- authorization code와 token API: https://developers.kakao.com/docs/en/kakaologin/rest-api
