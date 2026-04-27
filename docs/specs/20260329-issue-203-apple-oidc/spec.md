# Spec Template

## 1. Feature Overview
- Purpose: Apple 로그인 OIDC 토큰의 `aud` 검증 방식을 단일 client id 비교에서 허용 목록 기반으로 확장하고, 실제 토큰 예외 원인을 추적 가능하도록 한다.
- Scope
  - In:
    - `AppleOidcService`의 `aud` 검증 로직 및 설정 바인딩
    - 새 설정 `security.apple.allowed-client-ids` (`APPLE_ALLOWED_CLIENT_IDS` 환경 변수) 추가
    - Token 예외 처리 방식 변경 (원인 보존)
    - Apple OIDC 관련 단위 테스트 추가 및 보강
    - Spec bundle 및 문서 업데이트
  - Out:
    - Apple token exchange/revoke flow 전반적인 리팩토링
    - 타 OIDC 공급자 로직 변경
    - 운영 구성/배포 자동화
- Expected Impact: 허용된 복수 client id를 안전하게 지원하며, 실제 오류 원인을 통해 모니터링/디버깅이 용이해진다. 기존 단일 client id 환경도 fallback 덕분에 영향을 받지 않는다.
- Stakeholder Confirmation: 2026-03-29 사용자 요청(ISSUE-203) 및 배경 설명을 근거로 구현 승인.

## 2. Domain Rules
- Rule 1: Apple 토큰 검증 시 `aud` 값은 설정된 허용 client id 목록 중 하나여야 한다.
- Rule 2: 허용 목록이 비어 있으면 기존 `security.apple.client-id` 값을 단일 허용값으로 사용하여 하위호환한다.
- Rule 3: 토큰 검증 중 발생한 `TokenException`은 원래의 에러 코드를 유지한 채 상위로 전달한다.
- Mutable Rules: 허용 목록은 환경에 따라 변경될 수 있다.
- Immutable Rules: `nonce` 일치 검증은 기존과 동일하게 필수이며, Apple token exchange/revoke용 client id 설정은 유지되어야 한다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 허용된 aud로 로그인 성공
  - Trigger: 모바일 앱이 Apple ID 토큰을 서버로 전달
  - Actor: 모바일 앱, `AppleOidcService`
  - Steps:
    1. 서버는 설정에서 허용 client id 목록을 읽는다.
    2. Apple 토큰을 파싱하고 `nonce`, `aud`를 검증한다.
    3. `aud`가 허용 목록에 포함되고 `nonce`가 일치하면 정상 처리한다.
  - Expected Result: 검증이 통과하고 이후 흐름이 기존과 동일하게 진행된다.

### Exception / Boundary Flow
- Scenario Name: 허용 목록 외 aud
  - Condition: 토큰의 `aud`가 허용 목록(또는 fallback client id)에 없다.
  - Expected Behavior: `TokenException`의 `TOKEN_INVALID_AUD`가 그대로 상위로 전달되어 실패 원인이 노출된다.

- Scenario Name: TokenException 발생 시 원인 유지
  - Condition: 토큰 파싱/검증 중 `TOKEN_INVALID_NONCE`, `TOKEN_INVALID_AUD`, `TOKEN_PARSE_ERROR` 등 예외 발생
  - Expected Behavior: catch 후 재매핑하지 않고 원본을 전달하여 상위 레이어가 정확한 코드를 관측한다.

## 4. Transaction / Consistency
- Transaction Start Point: 해당 없음 (외부 토큰 검증)
- Transaction End Point: 해당 없음
- Atomicity Scope: 단일 토큰 검증 프로세스
- Eventual Consistency Allowed: N/A

## 5. API List (필요 시)
- Endpoint: N/A (내부 Apple OIDC 서비스)

## 6. Exception Policy
- Error Code: `TOKEN_INVALID_AUD`
  - Condition: 토큰 `aud`가 허용되지 않은 client id일 때
  - Message Convention: "%s is not allowed audience"
  - Handling Layer: `AppleOidcService`에서 throw → 상위 auth 흐름에서 처리
  - User Exposure: 최종 사용자에게는 일반 인증 실패 메시지를 제공하되, 내부 로깅/모니터링에는 정확한 코드가 남는다.
- Error Code: `TOKEN_INVALID_NONCE`
  - Condition: 기존 nonce 검증 실패
  - Message Convention: 기존 유지
  - Handling Layer / Exposure: 동일
- Error Code: `TOKEN_PARSE_ERROR`
  - Condition: 토큰 파싱 실패 시 그대로 전달

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Domain complete
- [ ] Phase 3 Application complete
- [x] Phase 4 Infrastructure complete
- [x] Phase 5 Global/Config complete
- [ ] Phase 6 API/Controller complete

## 8. Generated File List
- Path: `src/main/java/com/chukchuk/haksa/infrastructure/oidc/AppleOidcService.java`
  - Description: 허용 목록 검증 로직 및 예외 전달 방식
  - Layer: Infrastructure (외부 OIDC 호출)
- Path: `src/main/resources/application.yml`
  - Description: `security.apple.allowed-client-ids` 설정 항목 추가
  - Layer: Global/Config
- Path: `src/test/java/com/chukchuk/haksa/infrastructure/oidc/AppleOidcServiceTests.java`
  - Description: 허용 목록 검증 단위 테스트 추가/보강
  - Layer: Test
- Path: `docs/specs/20260329-issue-203-apple-oidc/*`
  - Description: Spec bundle 문서
  - Layer: Docs
