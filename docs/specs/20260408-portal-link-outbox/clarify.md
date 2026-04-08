# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | `spring.jpa.open-in-view`를 shadow/dev 에서 false로 내려도 기존 API 응답이 깨지지 않는지 QA 필요 | Backend | Open |
| 2 | ScrapeJobOutboxDispatcher gauge를 Micrometer long task timer로 대체할 수 있는지 | Infra | Open |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | develop-shadow는 outbox scheduler 비활성 조건에서 afterCommit 실패 시 전용 async executor 재시도 로직을 추가한다 | Shadow 환경에서 즉시 재시도 경로가 없어 장애 지속 | 2026-04-08 |

## Risks / Unknowns
- Item: Gauge 제거 시 운영 지표 공백
  - Impact: Scrape job backlog 모니터링 지연
  - Mitigation: Micrometer meter 를 lazy registration + async fetch 로 대체, 운영팀과 지표 공백 시간 합의

## Follow-ups
- [ ] QA가 shadow/dev에서 open-in-view false 적용 영향 테스트 (QA)
- [ ] 운영팀과 새로운 gauge/metric 설계 정리 (Infra)
