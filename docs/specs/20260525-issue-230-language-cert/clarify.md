# Issue 230 Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | `flangPassGb` 값 형식 | User | Resolved |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | `flangPassGb`는 `Y/N`으로 파싱한다. | 사용자 확인. | 2026-05-25 |
| 2 | 기존 사용자 미동기화 상태는 `languageCertFulfilled: null`, `languageCertNeedsRefresh: true`로 응답한다. | 미통과와 미확인을 구분하기 위함. | 2026-05-25 |
| 3 | 백그라운드 backfill은 이번 범위에서 제외한다. | 사용자 주도 새로고침으로 처리. | 2026-05-25 |

## Risks / Unknowns
- Item: 기존 진행 중인 스크래핑 payload에 `flangPassGb`가 없을 수 있다.
  - Impact: 해당 동기화에서는 외국어 인증 값이 저장되지 않는다.
  - Mitigation: null은 미동기화 상태로 남기고 졸업 요건 조회에서 새로고침 필요로 표시한다.

## Follow-ups
- [ ] 운영 backfill이 필요하면 별도 이슈로 분리한다.
