# Feature Development Skill

Use this skill when the user requests new behavior or extension of existing behavior.

---

## 1. Trigger

- Keywords: `기능 개발`, `새 기능`, `기능 추가`, `구현`, `확장`, `엔드포인트 추가`
- Intent: Add or change externally observable behavior.

---

## 2. Required Inputs

- Approved Context document (`docs/context/<id>.md` or equivalent)
- Domain rules and exception policy from `context.md`
- Affected layer list (domain/application/infrastructure/global/controller)

Without Context, stop and follow questioning rules.

---

## 3. Execution Sequence

1. Context fixed and confirmed
2. Write failing tests first (behavior-focused)
3. Implement in workflow order from `WORKFLOW.md`
4. Run `./gradlew test` (and `./gradlew build` when config/API touched)
5. Prepare commit with branch-aware Korean message format

---

## 4. Guardrails

- Do not change business logic outside the approved Context.
- Do not change external API contract without explicit reason in Context.
- Keep transactional integrity intact.
- Prefer minimal change that satisfies the requirement.

---

## 5. Exit Criteria

- Tests pass
- Generated files match Context artifact
- Change summary can be mapped to Context checklist
