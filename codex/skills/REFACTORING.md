# Refactoring Skill

Use this skill when the user requests structural improvement without changing behavior.

---

## 1. Trigger

- Keywords: `리팩토링`, `구조 개선`, `중복 제거`, `가독성 개선`, `정리`
- Intent: Improve maintainability while preserving outputs.

---

## 2. Required Inputs

- Refactoring scope and non-goals
- Behavior invariants (what must not change)
- Existing tests covering the impacted flow

If invariants are unclear, stop and ask with questioning rules.

---

## 3. Execution Sequence

1. Record refactoring Context (scope, invariants, excluded changes)
2. Add/adjust characterization tests when coverage is weak
3. Refactor in small reversible units
4. Run full impacted tests after each unit
5. Run `./gradlew test` before commit

---

## 4. Guardrails

- No intended behavior change unless explicitly approved.
- No public API contract change unless explicitly approved.
- Keep layering and dependency direction rules intact.
- Prefer simpler and more explicit code over abstraction growth.

---

## 5. Exit Criteria

- Before/after behavior proven equivalent by tests
- Reduced complexity or duplication is demonstrable
- No architecture/layer rule violation
