# Plan Template

## Architecture / Layering
- Domain impact: 없음 (토큰 검증 규칙은 인프라 서비스에서 처리)
- Application orchestration: 영향 없음, 예외 전달만 변경됨을 공유
- Infrastructure touchpoints: `AppleOidcService`에서 허용 목록 로딩/검증, TokenException 전달 방식 조정
- Global/config changes: `application.yml`에 `security.apple.allowed-client-ids` 기본값 추가 및 `@ConfigurationProperties` 바인딩 업데이트

## Data / Transactions
- Repositories touched: 없음
- Transaction scope: 토큰 검증은 외부 API 호출과 메모리 내 검증에 한정, 트랜잭션 사용 안 함
- Consistency expectations: 허용 목록 변경 시 재배포/재기동 필요, 설정 누락 시 fallback 단일 client id 사용

## Testing Strategy
- Domain tests: 해당 없음
- Application tests: 기존 상위 레이어 영향 없으므로 생략
- Integration/API tests: `AppleOidcServiceTests`에서 허용 목록 포함/미포함/비어있는 fallback/nonce 유지 케이스 검증
- Additional commands: `./gradlew test --tests com.chukchuk.haksa.infrastructure.oidc.AppleOidcServiceTests`, 가능하다면 `./gradlew test`

## Rollout Considerations
- Backward compatibility: 허용 목록이 비어 있어도 기존 `client-id` 값으로 자동 fallback되어 호환 유지
- Observability / metrics: TokenException 코드가 원본대로 전달되므로 로깅/모니터링에서 즉시 구분 가능
- Feature flags / toggles: 필요 없음, 환경 변수로 제어
