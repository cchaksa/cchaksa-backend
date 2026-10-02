# 관리자 로컬 인증·인가 및 세션 설계

## 경계와 피벗

관리자 인증은 일반 사용자 `UserService`, Kakao OIDC, 사용자 JWT principal과 refresh token을 사용하지 않는다. 관리자 권한과 로그인 자격증명의 정본은 개발진이 직접 등록하는 `admin_accounts`이며, `users`와 `social_accounts`를 참조하지 않는다. 일반 사용자 Kakao 인증 구현과 설정은 변경하지 않는다.

초기 #352의 관리자 Kakao challenge/callback 구현은 히스토리로 보존하되 최종 공개 계약에서는 제거한다. 관리자 웹과 일반 서비스의 수명주기가 다르고, 관리자별 자격증명을 개발진이 직접 발급한다는 변경된 제품 결정을 반영해 `loginId/password` 인증으로 피벗한다.

## 로그인과 CSRF 계약

1. `GET /api/admin/auth/csrf`는 인증 없이 호출할 수 있고 204와 `Cache-Control: no-store`를 반환한다. Path가 `/`인 `XSRF-TOKEN`이 없으면 발급하고 기존 유효 쿠키가 있으면 유지한다.
2. `POST /api/admin/auth/signin`은 `{loginId, password}`와 `X-XSRF-TOKEN`을 받는다. 두 값은 trim, 소문자 변환이나 문자 조합 검사를 하지 않고 입력 그대로 사용한다. null, empty, 공백-only는 거부한다. DB와 요청 크기 경계를 위한 기술적 상한은 loginId 255자, password 256자이며 비밀번호 복잡도 정책으로 취급하지 않는다.
3. 서버는 대소문자를 구분하는 exact loginId로 ACTIVE `admin_accounts`를 조회하고 BCrypt strength 12 hash를 검증한다. 미등록, 비밀번호 불일치와 비활성 계정은 모두 같은 401 `A13`으로 응답한다. 미등록 계정에도 dummy BCrypt 검증을 수행해 단순 timing 계정 열거를 완화한다.
4. 성공 시 불투명 세션 난수를 `cchaksa_admin_session` Secure, HttpOnly, SameSite=Strict 쿠키로 발급하고 DB에는 SHA-256 hash만 저장한다.
5. `GET /api/admin/auth/me`는 `adminAccountId`, `displayName`, `adminRole`을 반환한다.
6. `POST /api/admin/auth/signout`은 현재 세션을 폐기하고 session과 CSRF 쿠키를 만료한 뒤 204를 반환한다.

관리자 인증은 Spring `HttpSession`을 만들거나 저장하지 않는다. `AdminSessionAuthenticationFilter`가 매 요청의 불투명 쿠키를 SHA-256 hash로 조회해 DB `admin_sessions`의 폐기·idle·absolute 만료와 `admin_accounts.status`를 검사한 뒤 요청 범위 `SecurityContext`만 구성한다. 관리자 체인은 request cache를 사용하지 않으며 security context를 요청 이후 저장하지 않는다. 따라서 Spring session fixation 보호에 의존하지 않고 signin마다 새 관리자 session token을 발급하며, 비밀번호 변경은 현재 token을 포함한 기존 관리자 session을 모두 폐기하고 새 token을 발급한다.

쿠키를 사용하는 모든 POST는 `credentials: include`와 `X-XSRF-TOKEN`이 필요하다. Host, Origin과 CORS는 인증 수단으로 사용하지 않는다.

## 비밀번호 변경

`POST /api/admin/auth/password`는 인증된 관리자에게 `{currentPassword, newPassword}`를 받는다. 현재 비밀번호가 다르면 400 `A14`, 새 비밀번호가 기존과 같으면 400 `A15`를 반환한다.

변경은 계정 행을 write lock으로 직렬화한다. 성공 트랜잭션은 새 BCrypt hash 저장, 해당 계정의 모든 세션 폐기, 현재 응답용 새 세션 생성을 함께 수행한다. 따라서 현재 브라우저는 새 쿠키로 로그인 상태를 유지하고 다른 브라우저 세션은 무효화된다. 응답 유실 시 사용자는 다시 로그인한다. CSRF 쿠키는 비밀번호 변경 때 회전하지 않으며 다음 POST에서도 현재 값을 사용한다.

## 데이터 모델과 호환성

- `admin_accounts`: UUID PK, nullable legacy `provider/social_id`, nullable `login_id/password_hash`, `display_name`, `admin_role`, `status`, `created_by`, `last_login_at`, timestamps.
- `login_id`는 입력 그대로 저장하고 대소문자를 구분하는 exact unique 제약을 두며 `login_id/password_hash`는 둘 다 있거나 둘 다 없어야 한다.
- `admin_sessions`: 기존 UUID PK, 관리자 FK, token hash, idle/absolute expiry와 revoked timestamp를 유지한다.

`origin/dev`의 최신은 V15이고 #352가 관리자 기본 테이블 V16을 추가하므로 이 변경은 순차적인 V17을 사용한다. 후속 #354에 아직 미병합·미적용 상태로 예약된 문의 migration은 V18로 이동해야 한다. #352가 V18을 먼저 사용하면 dev에 선행 배포된 뒤 V17이 추가될 때 Flyway out-of-order가 발생하므로 사용할 수 없다. V17은 `login_id/password_hash`를 nullable로 추가하고 legacy `provider/social_id`의 NOT NULL만 완화한다. 기존 컬럼, unique/check 제약과 `admin_login_challenges` 테이블은 삭제하지 않는다. 따라서 migration이 먼저 적용되고 이전 Lambda Alias가 계속 요청을 처리해도 기존 Kakao 관리자 행과 challenge 흐름은 동작한다. 새 코드는 local credential 행만 로그인에 사용한다.

## 계정 초기 발급

자동 가입 API와 관리자 계정 CRUD API는 제공하지 않는다. 운영자는 승인된 password manager로 초기 비밀번호를 생성하고 다음 절차를 따른다.

1. 로컬 터미널에서 `htpasswd -nBC 12 admin`을 실행하고 프롬프트에 초기 비밀번호를 두 번 입력한다. 비밀번호를 process argument와 shell history에 남기는 `-b` 옵션은 사용하지 않는다.
2. 출력의 `admin:` 뒤 BCrypt hash만 승인된 DB 관리 도구의 bind parameter로 전달한다. loginId, UUID, 표시명, 역할, 상태와 생성 주체도 bind parameter로 insert하거나 기존 Kakao 행에 backfill한다.
3. 원문 비밀번호는 SQL, 출력 파일, 로그, 이슈, 문서와 Git에 넣지 않고 별도 보안 채널로 당사자에게 한 번만 전달한다. 임시 clipboard를 사용했다면 전달 직후 지운다.

Spring Security는 `$2a$`, `$2b$`, `$2y$` BCrypt 형식을 검증할 수 있으며 cost 12를 사용한다. 실제 DB 반영 전에는 non-production에서 생성 hash로 로그인과 비밀번호 변경을 검증한다. 현재 저장소에는 plaintext를 받거나 출력하는 계정 생성 endpoint/CLI를 추가하지 않는다.

기존 Kakao 관리자 행은 자동 변환하지 않는다. 운영자가 각 관리자에게 새 loginId와 초기 비밀번호를 발급하고 같은 행에 credential hash를 명시적으로 backfill해야 한다.

## 무차별 대입 방어

Lambda 인스턴스 메모리 카운터는 인스턴스 간 일관성과 재시작 내구성이 없어 신뢰 가능한 제한 수단이 아니다. 이 변경은 통합 오류와 dummy BCrypt 검증으로 계정 열거를 완화한다. IP/경로 rate limit은 API Gateway 또는 AWS WAF에서 적용해야 하며, 분산 공격과 credential stuffing 방어는 배포 후속 위험으로 남는다. 계정 lockout은 공격자가 정상 계정을 잠그는 DoS 정책 결정이 필요하므로 이번 범위에는 포함하지 않는다.

## 오류 계약

실패 응답은 `{ "success": false, "error": { "code": string, "message": string } }`이며 `details`가 없으면 생략한다.

- body validation 실패: 400 `C01`.
- 미등록, 비밀번호 불일치, 비활성 관리자: 401 `A13`.
- 현재 비밀번호 불일치: 400 `A14`.
- 새 비밀번호가 기존과 동일: 400 `A15`.
- 관리자 session 없음, 만료 또는 회수: 401 `A05`.
- CSRF 누락·불일치 또는 관리자 역할 없음: 403 `C04`.

## 민감정보

password 원문과 hash, session 원문을 응답, 애플리케이션 로그, 예외 메시지, Sentry tag, 문서와 Git에 기록하지 않는다. DTO의 `toString`을 로그에 남기지 않고 request body 로깅도 금지한다.
