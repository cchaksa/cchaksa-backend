# Context Notes

## 2026-06-15

- 사용자는 빠른 완화와 근본 해결을 분리하길 원했다.
- #263은 빠른 완화 이슈로 유지하고, #264는 기기별 세션 토큰 저장 구조 도입 이슈로 만들었다.
- #265를 상위 이슈로 만들고 #263, #264를 체크리스트로 연결했다.
- 현재 작업은 #263 빠른 완화만 다룬다.
- 현재 브랜치는 `feat/263`이고 `origin/dev` 기반으로 생성했다.
- 기존 워크트리에는 사용자 변경으로 보이는 `.codex/config.toml` 수정과 `docs/lambda-migration-backend-lessons.md` 미추적 파일이 있다. 이번 작업에서는 건드리지 않는다.
- 빠른 완화에서는 DB 구조를 바꾸지 않는다.
- refresh API에서 access token은 항상 새로 발급하고, refresh token은 저장된 `expiry`가 임계값 이하로 남은 경우에만 새로 발급한다.
- 로그인 API에서도 같은 refresh token 임계값 정책을 적용한다.
- 로그인 시 기존 refresh token이 있고 임계값보다 많이 남아 있으면 기존 token을 반환한다.
- 로그인 시 기존 refresh token이 없거나 임계값 이하로 남아 있으면 새 token을 저장한다.
- 임계값 설정 키는 `security.jwt.refresh-renewal-threshold`로 둔다.
- 공통 기본값은 7일인 `604800000`ms이고, dev profile은 2일인 `172800000`ms로 override한다.
- RED 확인: `./gradlew test --tests com.chukchuk.haksa.domain.auth.service.RefreshTokenServiceUnitTests.reissue_refreshExpiryBeyondThreshold_returnsExistingRefreshToken`가 기존 구현에서 실패했다.
- GREEN 확인: 동일 targeted test, `RefreshTokenServiceUnitTests` 전체, `./gradlew test`가 통과했다.
- 추가 RED 확인: `RefreshTokenServiceUnitTests.issueForSignIn_refreshExpiryBeyondThreshold_returnsExistingRefreshToken`가 `issueForSignIn` 메서드 부재로 실패했다.
- 추가 GREEN 확인: 로그인용 refresh token 발급 테스트와 `UserServiceUnitTests`가 통과했다.
- PR #266 리뷰에서 저장된 refresh token의 `expiry`가 null이면 `shouldRenewRefreshToken`에서 NPE가 날 수 있다는 지적이 있었다.
- `expiry`가 null인 refresh token은 남은 만료기간을 신뢰할 수 없으므로 갱신 대상으로 처리한다.
