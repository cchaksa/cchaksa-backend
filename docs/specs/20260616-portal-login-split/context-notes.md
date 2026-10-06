# 포털 연동 로그인 선검증 Context Notes

## 2026-06-16

- 현재 루트 worktree는 `feat/263`이고 사용자 변경 파일이 남아 있어 `/private/tmp/haksa-feat-267`에 격리 worktree를 만들었다.
- `feat/267`은 최신 `origin/dev` 기준으로 생성했다.
- 현재 `POST /portal/link`는 포털 자격 증명을 받아 스크래핑 job을 만들고 202를 반환한다.
- 로그인 검증 실패가 job 실행 경로에 묶이는 UX 문제를 줄이기 위해 `POST /portal/link` 내부에서 로그인 선검증을 수행하는 방향으로 진행한다.
- 스크래퍼 또는 Lambda 쪽 `POST /login` 경량 검증 경로가 필요하며, 이번 backend 변경은 그 경로를 전제로 한다.
- baseline으로 `PortalLinkJobServiceUnitTests`와 `PortalLinkControllerApiIntegrationTest`를 실행했고 통과했다.
- RED 테스트로 `PortalLinkJobServiceUnitTests`에 로그인 선검증 순서와 검증 실패 시 job 미생성 케이스를 추가했고, `PortalLinkJobService` constructor가 `PortalLoginVerifier`를 받지 않아 컴파일 실패하는 것을 확인했다.
- `PortalLinkJobService`가 요청 검증 후 `PortalLoginVerifier.verify(...)`를 먼저 호출하고, 성공한 경우에만 user 조회와 job 생성을 진행하도록 변경했다.
- `PortalClient.validateLogin`은 `crawler.base-url + /login`으로 POST 요청을 보내며, 401은 기존 `PORTAL_LOGIN_FAILED`로 매핑한다.
- 관련 테스트 묶음은 통과했다.
- 광고 UX가 추가되면서 단일 `/portal/link` 선검증 구조는 실패 사용자에게 광고가 노출될 수 있어 부적합하다고 판단했다.
- 최종 흐름은 `POST /portal/login`으로 ID/PW를 먼저 검증하고, 성공 시 `portal_verification_token`을 반환하는 구조다.
- 클라이언트는 `POST /portal/link`를 호출해 202 응답을 받은 직후 광고를 보여준다.
- `POST /portal/link`는 포털 ID/PW와 `portal_verification_token`을 함께 받고, token 검증 성공 시에만 job을 만든다.
- token은 서버 저장소 없이 `jjwt` 기반 HMAC 서명 JWT로 발급한다.
- token payload에는 비밀번호 원문이나 비밀번호 기반 fingerprint를 넣지 않는다.
- 비밀번호 일치 여부는 `/portal/link`에서 다시 제출된 비밀번호로 token 서명을 재계산해 확인한다.
- `/portal/login`, token service, `/portal/link` token 검증, OpenAPI 테스트를 추가했고 관련 테스트 묶음은 통과했다.
- `./gradlew test`와 `git diff --check`가 통과했다.
