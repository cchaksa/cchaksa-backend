# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 실제 응답 body를 변경해야 하는가? | Codex | Resolved |
| 2 | 포털 링크 외 전체 API의 확인된 불일치도 같은 branch에서 해결해야 하는가? | User | Resolved |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 실제 응답은 유지하고 Swagger schema만 실제 구조에 맞춘다. | 사진과 live `/v3/api-docs`에서 문서 schema가 내부 DTO만 참조하는 것이 원인으로 확인됐다. | 2026-05-29 |
| 2 | `*/*`로 보이는 성공/오류 응답 content type은 포털 링크 docs에서 `application/json`으로 명시한다. | Swagger UI의 media type 표시도 사용자 혼란을 만든다. | 2026-05-29 |
| 3 | 같은 `feat/246`에서 확인된 전체 Swagger/실제 응답 불일치를 추가로 해결한다. | 사용자가 “이것도 feat/246에서 이 이슈 해결”을 요청했다. | 2026-05-29 |
| 4 | 공개 API는 전역 bearerAuth 상속을 제거하고, 보호 API는 operation 단위 `@SecurityRequirement`로 표시한다. | 실제 SecurityConfig에서 public endpoint는 permitAll이고 보호 API만 인증이 필요하다. | 2026-05-29 |

## Risks / Unknowns
- Item: 인증 토큰과 데이터가 필요한 보호 API의 200 body는 live로 모두 호출하지 못했다.
  - Impact: 데이터 상태별 세부 필드 값까지는 Swagger와 전수 비교하지 못할 수 있다.
  - Mitigation: 컨트롤러 반환 타입, wrapper schema, 공통 오류 응답, 공개 호출 가능한 live 응답으로 검증한다.

## Follow-ups
- [ ] 배포 후 live `/v3/api-docs`에서 추가 불일치 항목이 사라졌는지 확인한다.
