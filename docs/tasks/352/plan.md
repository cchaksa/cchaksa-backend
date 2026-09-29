# 관리자 인증·인가 및 세션 구현 계획

## 1. 데이터와 카카오 검증

- V16 migration으로 관리자 계정, challenge, 세션 테이블과 제약·인덱스를 추가한다.
- 관리자 entity/repository와 난수·해시 컴포넌트를 구현한다.
- 카카오 JWKS/서명 검증을 중립 컴포넌트로 추출하고 사용자 로그인 회귀를 유지한다.

## 2. 인증 흐름

- challenge 발급·소모, 관리자 전용 Kakao 검증, 허용 목록 조회, 세션 생성·폐기 서비스를 구현한다.
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

## Wiki

인증, DB, 보안 운영 절차와 관리자 계정 직접 등록 방법의 Wiki 갱신이 필요하다. 저장소 구현 완료 결과에 별도 Wiki 갱신 필요를 기록한다.

