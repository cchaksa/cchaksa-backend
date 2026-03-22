## Summary
- Symptom: dev shadow Lambda에서 `ScrapeJobOutboxDispatcher`가 `CannotCreateTransactionException`, `Connection reset`으로 실패해 `scrape.outbox.sent`가 찍히지 않는다.
- Expected: `POST /portal/link` 직후 outbox dispatch가 안정적으로 한 번 실행되어 SQS publish까지 이어져야 한다.
- Actual: Lambda 내부 `@Scheduled` task가 warm 컨테이너의 stale Hikari connection을 재사용하면서 DB connection acquisition 단계에서 무너진다.
- Scope: scheduler 활성화 조건, outbox dispatch 호출 방식, Hikari pool 설정, dispatcher 관측 로그

## Hypothesis
- Lambda에서 상주 `@Scheduled` task를 유지하는 구조가 Supabase pooler와 맞지 않는다.
- 요청 기반 웹 Lambda와 백그라운드 scheduler가 같은 Hikari pool을 장시간 공유해 stale connection을 재사용한다.
- dispatcher의 트랜잭션 시작 전에 connection acquisition이 실패해 현재 로그만으로는 원인 구분이 어렵다.
