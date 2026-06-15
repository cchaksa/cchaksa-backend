# Spec Lite

## Overview

- Issue: #263.
- Parent issue: #265.
- Purpose: refresh API 호출 때마다 refresh token을 재발급해 저장하지 않고, 만료가 가까운 경우에만 갱신한다.
- Scope in:
  - `RefreshTokenService.reissue()`의 refresh token 갱신 조건.
  - prod 기본 임계값 7일.
  - dev profile 임계값 2일.
  - 관련 단위 테스트.
- Scope out:
  - 기기별 또는 세션별 refresh token 저장 구조.
  - 로그인 API의 refresh token 저장 정책 변경.
  - DB 스키마 변경.
  - 공개 API 필드 변경.

## Behavior

- refresh token 검증, 저장 token 일치 여부 확인, 사용자 조회는 기존 순서를 유지한다.
- access token은 refresh API 호출마다 새로 발급한다.
- 저장된 refresh token의 `expiry`가 현재 시각 기준 갱신 임계값보다 많이 남아 있으면 기존 refresh token을 응답한다.
- 저장된 refresh token의 `expiry`가 현재 시각 기준 갱신 임계값 이하로 남아 있으면 새 refresh token을 발급하고 저장한 뒤 응답한다.
- prod 기본값은 7일이다.
- dev profile 값은 2일이다.

## Risks

- 이 작업은 빠른 완화책이다. `refresh_token`이 `userId` 단일 row로 저장되는 구조는 그대로 유지한다.
- 여러 기기 동시 로그인의 근본 해결은 #264에서 별도 브랜치로 처리한다.

## Verification

- `RefreshTokenServiceUnitTests`에 기존 refresh token 유지 케이스를 추가한다.
- 기존 refresh token 갱신 케이스가 계속 통과하는지 확인한다.
- 관련 단위 테스트를 먼저 실행하고, 이후 전체 테스트를 실행한다.
