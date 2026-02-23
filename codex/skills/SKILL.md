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
   - `context.md`
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

- All feature development must be based on written Context documents.
- Without Context, no work starts.
- Codex must not infer or decide without updating Context.
- Requirements not explicitly written in Context are treated as nonexistent.

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
- The following are explicitly forbidden:
  - Implementing before tests
  - Editing code without creating a branch
  - Ending work without a commit

---

## 6. Questioning Rules (Fallback)

If Context is unclear or conflicts:

- Do not start work.
- Stop code changes, branch creation, and commits.
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
  Example: `feat/156`
- Commit messages must include the branch number.
- Commit messages must be written in Korean.
  - Format: `{branch} {type}: {message}`
    - Example: `156 feat: 게시판 기능 개발`
- One PR per Context.
- Auto-generated PRs must include:
  - Context summary
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
