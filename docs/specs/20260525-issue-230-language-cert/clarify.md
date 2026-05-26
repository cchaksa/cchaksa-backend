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
| 4 | 외국어 인증 기준은 `Department`에 직접 붙이지 않고 별도 정책 엔티티로 관리한다. | 학과 개편과 학번 구간별 기준을 명시적으로 관리하기 위함. | 2026-05-26 |
| 5 | 이번 구현은 엔티티와 enum까지 진행한다. | API와 seed 데이터는 별도 작업 단위로 분리한다. | 2026-05-26 |
| 6 | 외국어 인증 기준 조회 API와 seed SQL을 이번 범위에 포함한다. | 사용자 승인으로 후속 계획을 적용한다. | 2026-05-27 |
| 7 | `INFERRED`와 `UNMAPPED`를 오류가 아닌 정상 조회 상태로 반환한다. | 자동 추론을 피하면서 사용자에게 매핑 상태를 명확히 보여주기 위함. | 2026-05-27 |

## Risks / Unknowns
- Item: 기존 진행 중인 스크래핑 payload에 `flangPassGb`가 없을 수 있다.
  - Impact: 해당 동기화에서는 외국어 인증 값이 저장되지 않는다.
  - Mitigation: null은 미동기화 상태로 남기고 졸업 요건 조회에서 새로고침 필요로 표시한다.

## Follow-ups
- [ ] 운영 backfill이 필요하면 별도 이슈로 분리한다.
- [x] 외국어 인증 기준 조회 API를 후속 작업으로 구현한다.
- [x] 사진 기준표와 CSV 매칭 결과를 seed 데이터로 적재하는 방식을 후속 작업으로 결정한다.
