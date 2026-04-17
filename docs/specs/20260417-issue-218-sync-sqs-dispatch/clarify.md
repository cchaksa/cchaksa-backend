# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | publish 성공 후 상태 반영 트랜잭션이 실패하면 outbox/job 보정 전략을 어디까지 둘지? | BE 팀 | Open |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 별도 Dispatcher Lambda는 이번 작업에서 도입하지 않고, 기존 API Lambda 안에서 SQS를 동기 발행한다. | 현재 `QUEUED` 고착의 직접 원인을 최소 변경으로 제거하기 위함 | 2026-04-17 |
| 2 | `after_commit + TaskExecutor/@Scheduled` 기반 outbox dispatch는 제거한다. | Lambda 환경에서 in-memory background 실행 보장이 약함 | 2026-04-17 |
| 3 | `scrape_jobs`와 `scrape_job_outbox` 저장은 유지하고, publish 성공 시에만 `RUNNING`/`SENT` 전이를 수행한다. | 기존 idempotency와 전달 상태 추적을 유지하기 위함 | 2026-04-17 |
| 4 | 2026-04-17 사용자 지시 "기존 Lambda에서 동기 SQS 발행하도록 작업 시작해"를 OK to implement로 기록한다. | Phase 2 진행 승인 | 2026-04-17 |
| 5 | Lambda 환경에서 `TaskScheduler`가 비활성화되면 `ScrapeJobOutboxMetricsReporter`도 생성하지 않는다. | reporter는 관측용 보조 빈이며 앱 부팅보다 우선순위가 낮다. | 2026-04-17 |

## Risks / Unknowns
- Item: DB commit 성공 후 SQS publish 이전에 Lambda가 비정상 종료될 수 있음
  - Impact: job/outbox는 저장됐지만 메시지가 실제로 발행되지 않을 수 있음
  - Mitigation: 실패 상태/로그를 남기고 후속 replay 정책을 별도 이슈로 관리
- Item: publish 성공 후 상태 반영 실패 시 job/outbox 상태 불일치
  - Impact: 메시지는 큐에 존재하지만 DB 상태가 `QUEUED`에 남을 수 있음
  - Mitigation: 상태 반영을 짧은 별도 트랜잭션으로 두고 실패 로그/운영 점검 포인트 추가
- Item: Lambda에서 scheduler 비활성화 시 관측용 reporter가 필수 빈으로 남아 앱 부팅을 깨뜨릴 수 있음
  - Impact: 배포된 Lambda version이 `Active`로 전이되지 못하고 alias 전환이 막힘
  - Mitigation: reporter를 `TaskScheduler` 존재 조건부 빈으로 전환하고 회귀 테스트 추가

## Follow-ups
- [ ] publish 성공 후 상태 반영 실패에 대한 운영 복구 절차 문서화 (Owner: BE 팀)
