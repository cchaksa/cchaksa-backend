# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 목표 성능 수치(16초 → ?)를 명시적으로 확인할 필요가 있는가? | PO | Closed – 목표 5초 이하로 가정 |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | Hibernate JDBC batch 설정을 전역으로 적용한다. | Bulk insert/delete가 주요 병목으로 지목되었기 때문 | 2026-03-27 |
| 2 | 수강 이력 삭제는 `deleteAllByIdInBatch`로 대체하고, 업데이트는 dirty checking에 맡긴다. | JPA가 동일 트랜잭션에서 관리 중이므로 별도 saveAll 불필요 | 2026-03-27 |

## Risks / Unknowns
- Item: 실제 지연 원인이 batch 외 다른 I/O(예: Course/Professor getOrCreate)일 가능성
  - Impact: 성능 개선폭이 제한될 수 있음
  - Mitigation: 단계별 로그/메트릭 추가하여 병목 지점을 추후 추적

## Follow-ups
- [ ] 포털 sync 단계별 실행 시간을 micrometer metric으로 계측 (Owner: TBD)
