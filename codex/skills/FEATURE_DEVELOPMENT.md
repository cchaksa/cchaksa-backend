# Feature Development Skill

Use this skill when the user requests new behavior or extension of existing behavior.

---

## 1. Trigger

- Keywords: `기능 개발`, `새 기능`, `기능 추가`, `구현`, `확장`, `엔드포인트 추가`
- Intent: Add or change externally observable behavior.

---

## 2. Required Inputs

- 승인된 Spec Bundle (`docs/specs/<date-slug>/spec.md` + `clarify.md` + `plan.md` + `tasks.md`)
- Domain rules and exception policy from `spec.md`
- Affected layer list (plan.md에서 정의)

Spec Bundle이 없으면 즉시 작업을 중단하고 Phase 1을 다시 수행한다. Lite 스펙은 Phase 2 진입 전에 Standard로 승격해야 한다.

---

## 3. Execution Sequence

1. Context fixed and confirmed
2. Write failing tests first (behavior-focused)
3. Update `tasks.md`에 작업/테스트 체크 항목을 기록하면서 `WORKFLOW.md` 순서대로 구현
4. Run `./gradlew test` (and `./gradlew build` when config/API touched) and log the command/result in `tasks.md`
5. Prepare commit with branch-aware Korean message format referencing the spec folder

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
