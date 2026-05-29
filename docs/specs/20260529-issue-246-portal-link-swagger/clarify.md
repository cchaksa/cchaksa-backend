# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 실제 응답 body를 변경해야 하는가? | Codex | Resolved |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 실제 응답은 유지하고 Swagger schema만 실제 구조에 맞춘다. | 사진과 live `/v3/api-docs`에서 문서 schema가 내부 DTO만 참조하는 것이 원인으로 확인됐다. | 2026-05-29 |
| 2 | `*/*`로 보이는 성공/오류 응답 content type은 포털 링크 docs에서 `application/json`으로 명시한다. | Swagger UI의 media type 표시도 사용자 혼란을 만든다. | 2026-05-29 |

## Risks / Unknowns
- Item: 다른 도메인 API에도 `*/*` media type이 남아 있을 수 있다.
  - Impact: 이번 이슈 범위를 벗어난 Swagger 표시 불일치가 남을 수 있다.
  - Mitigation: 사용자 사진과 live mismatch가 확인된 포털 링크 API 범위로 제한한다.

## Follow-ups
- [ ] 필요하면 별도 이슈로 전체 API Swagger media type 정리를 진행한다.
