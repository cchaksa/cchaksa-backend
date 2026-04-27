# Spec Lite Template

## Summary
- Purpose: origin/dev 기준으로 깨지는 테스트(새 생성자 시그니처 미반영)를 최신 구조에 맞게 수정한다.
- Scope (In/Out):
  - In: KakaoOidcServiceAudValidationTests, UserServiceUnitTests가 참조하는 DTO/서비스 생성자 시그니처 반영
  - Out: 프로덕션 코드, 다른 테스트 클래스, CI workflow 수정
- Expected Impact: `./gradlew test`가 dev 최신 상태에서 재성공하며 Lambda/OIDC 관련 리팩토링 이후에도 회귀 없이 검증할 수 있다.

## Key Rules
- Rule 1: 테스트는 실제 프로덕션 시그니처와 동일한 인자를 사용해야 하며 모의 객체로 대체할 수 없는 의존성은 명시적으로 주입한다.
- Rule 2: 테스트 수정으로 기존 행위 검증 시나리오(예: AUD 검증, sign-in happy path) 자체는 변경하지 않는다.

## Risks / Assumptions
- Risk: KakaoOidcService가 추가 인자를 더 요구할 경우 다른 테스트도 연쇄적으로 실패할 수 있다.
- Assumption: OidcJwksClient 의존성은 Mockito mock으로 주입해도 동작에 영향이 없다.

## Tasks
- [x] Tests planned
- [x] Implementation plan agreed
- [x] Verification command listed (2026-03-27 15:19 KST, `./gradlew test`, PASS)

> Lite 스펙은 Scope < 1 day & API 변경 없음일 때만 허용. 조건을 벗어나면 즉시 Standard 스펙으로 승격한다.
