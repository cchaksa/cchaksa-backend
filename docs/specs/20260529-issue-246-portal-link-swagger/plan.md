# Plan

## Architecture / Layering
- Domain impact: 없음.
- Application orchestration: 없음.
- Infrastructure touchpoints: 없음.
- Global/config changes: 전역 bearerAuth 적용을 제거하고, operation security 기준으로 공통 401 응답과 media type을 보정한다.
- API documentation: 포털 링크 성공 응답용 wrapper와 내부 콜백 성공 wrapper를 문서화하고, health/sentry-test의 실제 응답을 명시한다.

## Data / Transactions
- Repositories touched: 없음.
- Transaction scope: 없음.
- Consistency expectations: 실제 JSON과 Swagger schema가 일치해야 한다.

## Testing Strategy
- Domain tests: 없음.
- Application tests: 없음.
- Integration/API tests: `/v3/api-docs`의 response schema, media type, security, 401 응답 문서화를 검증한다.
- Additional commands: OpenAPI contract test, 포털 링크/콜백 관련 컨트롤러 테스트, 전체 테스트를 실행한다.

## Rollout Considerations
- Backward compatibility: 런타임 응답 body를 바꾸지 않으므로 클라이언트 호환성 영향 없음.
- Observability / metrics: 영향 없음.
- Feature flags / toggles: 없음.
