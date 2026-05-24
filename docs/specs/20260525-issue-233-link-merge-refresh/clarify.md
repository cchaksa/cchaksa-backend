# Clarify Template

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 병합 후 이미 연동된 LINK를 어떤 정책으로 처리할지? | User | Resolved |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 병합 후 active user가 이미 `portalConnected=true`이면 REFRESH처럼 재동기화한다. | 기존 연동 계정 병합 케이스는 신규 초기화가 아니라 기존 연결 갱신에 가깝고, 사용자가 명시적으로 REFRESH처럼 봐야 한다고 결정했다. | 2026-05-25 |
| 2 | 스크래퍼, S3 콜백 payload, 클라이언트 API 계약은 수정하지 않는다. | 로그상 스크래퍼 성공 콜백과 S3 payload 수신은 정상이고 실패는 백엔드 후처리 분기에서 발생했다. | 2026-05-25 |

## Risks / Unknowns
- Item: 이미 `FAILED`로 남은 기존 job 재처리.
  - Impact: 코드 수정만으로 과거 실패 job이 자동 복구되지는 않는다.
  - Mitigation: 이번 범위에서는 신규 재시도 정상화만 보장하고, 기존 실패 job 재처리는 별도 운영 판단으로 남긴다.

## Follow-ups
- [ ] 배포 후 동일 로그 패턴이 사라졌는지 Sentry 또는 애플리케이션 로그로 확인한다. (Owner: 운영자)
