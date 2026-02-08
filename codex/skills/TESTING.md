# Testing

This document defines the standards Codex must follow when writing tests.

---

## Purpose
- Ensure stability of changes.
- Fix requirements and domain rules in an executable form.

---

## Scope
- Unit tests
- Integration tests
- Layer-specific tests

---

## Principles
- Tests verify behavior.
- Do not over-depend on implementation details.
- Make failure causes explicit.
- Include tests directly tied to changed functionality.

---

## Prohibitions
- Tests that only inflate coverage without meaning
- Tests with uncontrolled external dependencies

---

## Deliverables
- Test code
- Test execution results (when needed)
