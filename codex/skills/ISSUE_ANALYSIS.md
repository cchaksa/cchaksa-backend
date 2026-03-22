# Issue Analysis Skill

Use this skill when the user requests investigation before implementation.

---

## 1. Trigger

- Keywords: `이슈 분석`, `원인 분석`, `왜`, `조사`, `영향도`, `옵션 비교`
- Intent: Build a defensible understanding and options before deciding changes.

---

## 2. Required Inputs

- Investigation question
- Scope and constraints
- Available evidence sources (logs, metrics, code paths, incidents)

If scope is missing, ask focused clarification before deep analysis.

---

## 3. Execution Sequence

1. Define question and boundaries
2. Collect evidence from code, tests, logs, and runtime signals
3. Build hypotheses and validate each with evidence
4. Produce conclusion with confidence level and known unknowns
5. Recommend next action:
   - no code change
   - move to `BUG_FIX.md`
   - move to `FEATURE_DEVELOPMENT.md`
   - move to `REFACTORING.md`

---

## 4. Guardrails

- Separate observed facts from inference.
- Do not modify business logic during analysis-only requests.
- Avoid speculative conclusions without evidence.
- Explicitly note risk if evidence is incomplete.

---

## 5. Output Template

- Question
- Scope
- Evidence
- Findings
- Root cause hypothesis
- Confidence and risks
- Recommended next step
