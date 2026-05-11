# Clarify Template

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 해당 없음 | - | Closed |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 미정의 이수 구분은 원본 문자열 저장 없이 `기타`로만 관리한다 | 데이터 모델 단순화 및 향후 값 추가 시에도 안전하게 처리 | 2026-04-07 |
| 2 | 졸업 요건 API `기타` 영역은 required=0, earned=실제 이수 학점 합계로 노출한다 | 요구사항 테이블을 건드리지 않고도 영역 식별 가능 | 2026-04-07 |
| 3 | 학업 기록 API는 `기타` 과목을 별도 리스트로 제공한다 | 클라이언트가 전공/교양과 분리 표시 가능 | 2026-04-07 |

## Risks / Unknowns
- Item: 포털에서 갑작스럽게 다수의 신규 이수 구분을 제공할 수 있음
  - Impact: `기타` 비중이 높아지고, 세부 분석 시 구분 불가
  - Mitigation: 추후 정렬된 목록을 확보한 후 enum 확장 + `clarify.md` 업데이트

## Follow-ups
- [ ] 포털 팀으로부터 전체 이수 구분 목록 재확인 (Owner: Product)
