# Bug Fix Skill

Use this skill when the user requests defect resolution.

---

## 1. Trigger

- Keywords: `버그`, `오류`, `에러`, `실패`, `수정`, `고쳐`, `핫픽스`
- Intent: Restore expected behavior for a defect.

---

## 2. Required Inputs

- Symptom and expected behavior
- Reproduction steps or evidence (logs, stacktrace, failing case)
- Impact scope (module, endpoint, user impact)

If reproduction is missing, first create a minimal reproducible condition.

---

## 3. Execution Sequence

1. Create bug Context with: symptom, expected/actual, scope, hypothesis
2. Reproduce with a failing test (or deterministic failing check)
3. Identify root cause
4. Apply minimal safe fix
5. Add regression test
6. Run `./gradlew test` (and targeted integration tests if needed)

---

## 4. Guardrails

- Fix root cause, not only symptom masking.
- Do not expand scope into unrelated refactoring.
- Preserve transaction integrity and external API contract.
- Keep rollback path clear with focused change units.

---

## 5. Exit Criteria

- Reproduction no longer fails
- Regression test added and passing
- Adjacent behavior remains stable in impacted test suite
