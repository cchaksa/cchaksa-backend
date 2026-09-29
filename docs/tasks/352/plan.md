# 관리자 인증·인가 및 세션 구현 계획

## 1. 데이터와 카카오 검증

- V16 migration으로 관리자 계정, challenge, 세션 테이블과 제약·인덱스를 추가한다.
- 관리자 entity/repository와 난수·해시 컴포넌트를 구현한다.
- 카카오 JWKS/서명 검증을 중립 컴포넌트로 추출하고 사용자 로그인 회귀를 유지한다.
- V16 challenge에 OAuth state와 브라우저 결합 token hash를 저장하고 JavaScript/REST API key, client secret, 고정 redirect URI 설정을 분리한다.

## 2. 인증 흐름

- challenge 발급·state/HttpOnly 브라우저 cookie 검증·소모, timeout이 적용된 서버 측 authorization code 교환, 관리자 전용 Kakao 검증, 허용 목록 조회, 세션 생성·폐기 서비스를 구현한다.
- challenge, signin, me, signout API와 Springdoc 계약을 추가한다.
- 관리자 principal과 쿠키 세션 인증 필터를 구현한다.

## 3. 보안 경계

- `/api/admin/**` 우선순위 체인에 Cookie CSRF와 관리자 authority 정책을 적용한다.
- USER Bearer token의 403, 미인증 401, 역할별 접근을 검증한다.
- 관리자 origin을 환경별 CORS 허용 목록에 명시하되 인증 수단으로 사용하지 않는다.

## 4. 검증

- 계정·challenge·세션 단위 테스트와 로그인/로그아웃/CSRF/회수 통합 테스트를 추가한다.
- migration, Springdoc 계약, 사용자 로그인 회귀와 `/v3/api-docs`를 검증한다.
- `./gradlew check --stacktrace --no-daemon`과 `git diff --check`를 통과한다.
- `352 feat: 관리자 인증과 세션을 구현`으로 커밋한다.

## 5. dev 로그인 설정 준비

- `DEV_ADMIN_KAKAO_JAVASCRIPT_APP_KEY`, `DEV_ADMIN_KAKAO_REDIRECT_URI` Actions variable과 `DEV_ADMIN_KAKAO_REST_API_KEY`, `DEV_ADMIN_KAKAO_CLIENT_SECRET` Actions secret을 dev Lambda 환경 변수로 연결한다.
- dev 배포에서 `ADMIN_AUTH_COOKIE_SECURE=true`를 고정하고 redirect URI가 HTTPS origin의 `/login/callback`인지 값 노출 없이 검증한다.
- `aws lambda update-function-configuration` 호출 전에 현재 Lambda 환경 변수를 조회하고 관리자 설정만 병합해 기존 설정을 보존한다.
- 필수 애플리케이션 설정이 비어 있으면 challenge 생성 전 일반 서버 오류로 중단하는 통합 테스트를 추가한다.
- #352 인증 테스트와 스택 후속인 #354 문의 테스트를 실행하고 Draft PR에 검증 결과와 미확정 dev 관리자 웹 도메인을 기록한다.

검증 결과:

- Java 17 `./gradlew check --stacktrace --no-daemon`: 성공.
- dev 배포 workflow YAML parse, 필수값 공백 검사, redirect URI 정상·오류 matrix와 기존 Lambda 환경 변수 병합 재현: 성공.
- `git diff --check`: 성공.
- 보안 민감정보 점검: 실제 Kakao key, client secret, redirect URI와 계정 값 노출 없음.
- 별도 검증 에이전트 Pre-PR 검증: 입력 공백과 URL host·port 검증을 두 차례 보완한 뒤 `pass`.
- GitHub Environment `dev`의 variable/secret 이름 목록은 현재 비어 있어 네 설정값 등록이 남아 있다.
- 실제 dev Lambda 배포와 Kakao 브라우저 로그인은 사용자 입력 후 dev 환경에서 검증하며 이번 변경에서는 실행하지 않았다.

## Wiki

인증, DB, 보안 운영 절차와 관리자 계정 직접 등록 방법의 Wiki 갱신이 필요하다. 저장소 구현 완료 결과에 별도 Wiki 갱신 필요를 기록한다.
