# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | `spring.jpa.open-in-view`를 shadow/dev 에서 false로 내려도 기존 API 응답이 깨지지 않는지 QA 필요 | Backend | Open |
| 2 | ScrapeJobOutboxDispatcher gauge를 Micrometer long task timer로 대체할 수 있는지 | Infra | Open |
| 3 | Student 중복 정리용 admin 스크립트가 필요한지 여부 | Backend | Pending |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | develop-shadow는 outbox scheduler 비활성 조건에서 afterCommit 실패 시 전용 async executor 재시도 로직을 추가한다 | Shadow 환경에서 즉시 재시도 경로가 없어 장애 지속 | 2026-04-08 |
| 2 | `/internal/scrape-results` 성공 판정은 포털 초기화/학업 동기화까지 완료되어야 하며, 실패 시 Job 을 FAILED 로 롤백한다 | 중복 Student 로 인한 유실/오탐 방지 | 2026-04-09 |
| 3 | 기존 Student 가 남아있는 경우 reset/update 로 재사용해 unique 제약을 피한다 | 재시도 시 Student duplicate 방지 | 2026-04-09 |

## Risks / Unknowns
- Item: Gauge 제거 시 운영 지표 공백
  - Impact: Scrape job backlog 모니터링 지연
  - Mitigation: Micrometer meter 를 lazy registration + async fetch 로 대체, 운영팀과 지표 공백 시간 합의
- Item: Student 중복 데이터가 남아 retry 시 unique 제약을 재발시킬 수 있음
  - Impact: Callback 성공 응답이 나갔는데 Job 은 failed 처리됨
  - Mitigation: tryMerge + Student 재사용, 필요 시 admin 스크립트 TODO 기록

## Follow-ups
- [ ] QA가 shadow/dev에서 open-in-view false 적용 영향 테스트 (QA)
- [ ] 운영팀과 새로운 gauge/metric 설계 정리 (Infra)
- [ ] Student 정합성 배치/스크립트 필요 시 요구사항 수집 (Backend)
