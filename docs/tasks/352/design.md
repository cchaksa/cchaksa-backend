# 관리자 인증·인가 및 세션 설계

## 경계

관리자 인증은 일반 사용자 `UserService`, 사용자 JWT principal, refresh token과 별도 흐름으로 구현한다. 관리자 권한의 정본은 `admin_accounts`이며 `users`와 `social_accounts`를 참조하지 않는다. 사용자 Bearer JWT는 관리자 API에서 관리자 권한으로 승격되지 않는다.

카카오 구현은 세 가지 대안을 비교했다.

1. 기존 `KakaoOidcService` 직접 재사용은 사용자 앱 key와 예외·수명주기를 결합하므로 제외한다.
2. 전체 구현 복제는 보안 수정이 두 군데로 갈라지므로 제외한다.
3. JWKS 조회와 카카오 토큰 서명·표준 claim 검증만 중립 컴포넌트로 공유하고, 사용자·관리자 서비스와 audience 설정은 분리한다.

3안을 사용한다. 관리자 설정은 `security.admin.kakao.app-key`와 별도 redirect URI를 사용한다.

## 로그인 계약

1. `GET /api/admin/auth/challenge`가 UUID `challengeId`와 난수 `nonce`를 발급한다. 응답은 캐시하지 않으며 `XSRF-TOKEN` 쿠키도 생성한다.
2. `POST /api/admin/auth/signin`은 `{challengeId, idToken}`과 `X-XSRF-TOKEN`을 받는다. 서버는 DB에 저장한 미사용 nonce를 원자적으로 소모한 뒤 카카오 ID Token을 검증한다.
3. `provider`와 `socialId`는 요청에서 받지 않는다. 검증된 `sub`로 ACTIVE `admin_accounts`를 조회한다.
4. 성공 시 불투명 세션 난수를 `cchaksa_admin_session` Secure, HttpOnly, SameSite=Strict 쿠키로 발급하고 DB에는 SHA-256 해시만 저장한다.
5. `GET /api/admin/auth/me`는 `adminAccountId`, `displayName`, `role`을 반환한다.
6. `POST /api/admin/auth/signout`은 현재 세션을 폐기하고 세션 쿠키와 CSRF 쿠키를 만료한다.

challenge는 짧게 만료되고 한 번의 로그인 시도에만 사용할 수 있다. 세션은 idle timeout과 absolute timeout을 모두 검사하며, 매 요청에서 계정 ACTIVE 상태를 재검증해 권한 회수를 즉시 반영한다.

## 모델

- `admin_accounts`: UUID PK, provider(KAKAO), social_id unique, display_name, admin_role(ADMIN/CS_AGENT), status(ACTIVE/INACTIVE), created_by, last_login_at, created_at, updated_at.
- `admin_login_challenges`: UUID PK, nonce, expires_at, used_at, created_at. nonce는 인증 토큰이 아니며 짧은 기간 뒤 삭제 가능한 일회성 값이다.
- `admin_sessions`: UUID PK, admin_account_id FK, token_hash unique, expires_at, idle_expires_at, last_accessed_at, revoked_at, created_at.

## 보안 체인

`/api/admin/**` 전용 우선순위 SecurityFilterChain을 둔다. challenge와 signin만 익명 접근을 허용하고 모든 상태 변경에 Cookie CSRF를 적용한다. 관리자 세션 필터는 `AdminPrincipal`에 `ROLE_ADMIN` 또는 `ROLE_CS_AGENT`를 부여한다. 일반 JWT 필터도 관리자 체인에서 실행하되 일반 principal에는 관리자 authority가 없으므로 USER token은 403이 된다. 미인증은 401이다.

Host, Origin, CORS는 권한 판단에 사용하지 않는다. CORS는 브라우저 전송 경계일 뿐이며 최종 인가는 DB 세션과 authority가 담당한다.

## 운영과 호환성

V16은 신규 테이블만 추가하므로 이전 Lambda와 호환된다. 기본 수명은 challenge 5분, idle 30분, absolute 8시간이며 환경 변수로 조정한다. 세션·nonce·ID Token 원문과 socialId는 로그나 Sentry tag에 남기지 않는다.
