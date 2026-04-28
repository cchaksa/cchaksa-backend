# 20260428 Issue 211 CI Reset Academic Data Fix Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 작업 브랜치에 매핑할 GitHub Issue 번호는 무엇인가? | User | Resolved: `211` |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 삭제된 `Student.resetAcademicData()`를 복구하지 않고 `StudentService.resetBy`를 사용한다. | 211 hotfix에서 연관관계 편의 메서드를 제거하고 명시 삭제 쿼리로 이동했기 때문이다. | 2026-04-28 |
| 2 | PR 대상 브랜치는 `dev`로 한다. | 사용자 요청. | 2026-04-28 |

## Risks / Unknowns
- Item: `resetBy`가 별도 repository delete 쿼리를 사용하므로 호출 순서가 중요하다.
  - Impact: 학생 정보 갱신 전에 이전 학사 데이터가 삭제되어야 한다.
  - Mitigation: `reuseExistingStudent` 시작 시 `studentService.resetBy(student.getId())`를 호출하고 테스트로 검증한다.

## Follow-ups
- [ ] PR 생성 후 GitHub Actions 재실행 결과 확인 (Owner: User/Codex)
