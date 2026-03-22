## Summary
- Symptom: dev shadow 비동기 스크래핑 E2E에서 worker가 `INVALID_PAYLOAD`와 callback `401`로 종료한다.
- Expected: `requested_at`이 worker 계약에 맞는 ISO-8601 문자열로 전달되고, worker callback이 HMAC 검증을 통과해야 한다.
- Actual: enqueue payload의 `requested_at`이 epoch 숫자로 직렬화되고, callback은 `INVALID_CALLBACK_SIGNATURE`로 거부되어 job이 `queued`에 머문다.
- Scope: `POST /portal/link`의 outbox payload 생성, `/internal/scrape-results` HMAC 검증, 관련 회귀 테스트

## Hypothesis
- `ScrapeJobMessage.requested_at`가 `Instant` 타입이라 기본 Jackson 설정에서 timestamp 숫자로 직렬화된다.
- callback secret이 hex 문자열로 주입되는데 백엔드는 UTF-8 문자열 그대로 HMAC key로 사용해 worker와 다른 canonical key bytes를 계산한다.
- callback 401 로그가 사유를 남기지 않아 실제 실패 원인 추적이 어렵다.
