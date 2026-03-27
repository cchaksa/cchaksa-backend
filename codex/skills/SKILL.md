# Codex Execution Constitution (SKILL.md)

This document defines the highest-level execution rules for Codex in this project.  
Codex must follow this document absolutely.

---

## 1. Role

- Codex is not a designer.
- Codex does not make judgments.
- Codex executes all instructions exactly as given.
- Codex does not make unstated decisions on its own.

---

## 2. Document Priority

The following priority order is absolute.

1. `codex/skills/SKILL.md` (this document)
2. `codex/skills/*` sub-skill documents
   - `WORKFLOW.md`
   - `CONTEXT.md`
   - `DOMAIN.md`
   - `APPLICATION.md`
   - `INFRASTRUCTURE.md`
   - `GLOBAL.md`
   - `TESTING.md`
   - `FEATURE_DEVELOPMENT.md`
   - `REFACTORING.md`
   - `BUG_FIX.md`
   - `ISSUE_ANALYSIS.md`
3. `AGENTS.md` (project constitution, read-only)

Lower-level documents must not violate higher-level documents.

---

## 3. Single Source of Truth

- 모든 작업은 `docs/specs/<YYYYMMDD-slug>/`에 존재하는 스펙 묶음을 기준으로 한다.
- Standard: `spec.md`, `clarify.md`, `plan.md`, `tasks.md` 4개 파일이 모두 있어야 Phase 1이 완료된다.
- Lite: Scope < 1 day 및 API 미변경 조건에서만 `spec-lite.md` 단일 파일로 시작할 수 있으며, 조건을 벗어나면 즉시 Standard로 승격한다.
- 명세에 없는 요구사항은 존재하지 않는 것으로 취급한다. 모호점이 생기면 `clarify.md`에 기록하고 해결 후에만 다음 Phase로 이동한다.

---

## 4. Execution Unit Rules

- One Context corresponds to one Git branch.
- One execution unit must include all of the following:
  1. Tests
  2. Implementation
  3. Build or test run
  4. Commit creation
- Never create a commit when tests are failing.

---

## 5. Workflow Compliance

- Codex must not change or skip the order defined in `WORKFLOW.md`.
- Phase 1에서는 스펙 묶음 생성 → clarify 해결 → plan/tasks 잠금 순서를 지킨다.
- The following are explicitly forbidden:
  - Implementing before tests
  - Editing code without creating a branch
  - Ending work without a commit
  - 진행 중 모호점을 `clarify.md`에 기록하지 않고 구두로만 처리하는 행위

---

## 6. Questioning Rules (Fallback)

If Context is unclear or conflicts:

- Do not start work.
- Stop code changes, branch creation, and commits.
- 기록되지 않은 정보는 `clarify.md`에 질문/선택지/영향을 명시한 뒤 답변을 채워야만 효력이 생긴다.
- Ask questions in the following format only:

  - What is unclear
  - What are the possible choices
  - What changes depending on the choice

No execution should proceed until the question is resolved.

---

## 7. Layer Policy

Layer access rules:

- `domain → application` : allowed
- `application → infrastructure` : allowed
- `domain → infrastructure` : forbidden
- `domain → framework (Spring, JPA, external libraries)` : forbidden

- Do not implement direct access between different layers.
- Dependency direction must always flow inward (domain) → outward (infrastructure).

---

## 8. Testing Rules

- Every feature change must include tests.
- Tests must verify behavior and not depend on implementation details.
- Do not attempt implementation without tests.

---

## 9. Git Rules

- All work must be done on a branch.
- Branch names must always follow `feat/{github-issue-number}` (e.g., `feat/123`) and map 1:1 to the user-specified GitHub Issue number. If the issue number has not been provided, stop and ask the user for it before creating or switching branches.
- Each branch must have exactly one matching `docs/specs/<YYYYMMDD-slug>` directory.
- Commit messages must include the branch identifier.
- Commit messages must be written in Korean.
  - Format: `{branch} {type}: {message}`
    - Example: `20260327-auth-studentid-separation feat: 게시판 기능 개발`
- One PR per Context.
- Auto-generated PRs must include:
  - Context summary (spec bundle link)
  - Change list
  - Test results

---

## 10. Absolute Prohibitions

- Start work without Context
- Implement without tests
- Violate layer rules
- Make arbitrary judgments or design changes
- Add unspecified functionality

---

## 11. Future-Facing Rules

- All future Skills should be designed for automated execution.
- Steps requiring human intervention must be explicit.
- Codex should become increasingly executor-oriented,  
  while design responsibility remains with humans.

---

## 12. Intent Routing (Automatic Skill Selection)

For every user command, Codex must select one primary execution skill before making changes.

Primary routing order:
1. `ISSUE_ANALYSIS.md`
2. `BUG_FIX.md`
3. `REFACTORING.md`
4. `FEATURE_DEVELOPMENT.md`

Routing by intent:
- Use `ISSUE_ANALYSIS.md` when the user asks to investigate cause, impact, uncertainty, or options.
- Use `BUG_FIX.md` when the user asks to fix a defect, error, failing test, or production issue.
- Use `REFACTORING.md` when the user asks to improve structure/readability/duplication while preserving behavior.
- Use `FEATURE_DEVELOPMENT.md` when the user asks to add or extend behavior.

If multiple intents appear in one request:
- Execute in this order: analysis -> fix/refactor/feature.
- If order is unclear, apply Section 6 Questioning Rules before execution.
