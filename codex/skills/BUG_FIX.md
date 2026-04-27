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
- Spec Bundle stub: `spec.md`에 증상, 범위, 실패 조건을 명시하고 `clarify.md`에 가설/추가 질문을 정리한다. 단시간 수정이면 `spec-lite.md`로 시작할 수 있으나 구현 전에 Standard로 전환한다.

If reproduction is missing, first create a minimal reproducible condition and log it inside `spec.md` + `tasks.md`.

---

## 3. Execution Sequence

1. Create bug Context (Phase 1) with: symptom, expected/actual, scope, hypothesis
2. Reproduce with a failing test (or deterministic failing check)
3. Identify root cause
4. Apply minimal safe fix following `WORKFLOW.md` order (Phase 2-6) while checking off `tasks.md`
5. Add regression test (recorded in `tasks.md`)
6. Run `./gradlew test` (and targeted integration tests if needed) and capture the command/result in `tasks.md`

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
