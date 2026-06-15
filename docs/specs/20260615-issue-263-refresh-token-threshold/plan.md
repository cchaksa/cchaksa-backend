# Plan

## Goal

Refresh API 호출 시 access token은 항상 새로 발급하되, refresh token은 만료 임계값 이하로 남은 경우에만 갱신한다.

## Steps

1. 현재 refresh token 재발급 흐름과 테스트 구조를 확인한다.
   - Verify: `RefreshTokenService.reissue()`와 `RefreshTokenServiceUnitTests`를 읽는다.
2. 만료 임계값보다 많이 남은 refresh token은 유지되는 RED 테스트를 추가한다.
   - Verify: targeted test가 기존 구현에서 실패한다.
3. `RefreshTokenService`에 저장된 `expiry` 기준 갱신 조건을 추가한다.
   - Verify: targeted test가 통과한다.
4. prod 기본 7일, dev 2일 설정을 추가한다.
   - Verify: `application.yml`, `application-dev.yml`에 설정값이 반영된다.
5. 관련 단위 테스트와 전체 테스트를 실행한다.
   - Verify: `./gradlew test --tests com.chukchuk.haksa.domain.auth.service.RefreshTokenServiceUnitTests`와 `./gradlew test`가 통과한다.
