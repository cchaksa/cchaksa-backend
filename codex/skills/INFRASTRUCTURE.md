# Infrastructure

This document defines the standards Codex must follow when working in the infrastructure layer.

---

## Purpose
- Handle technical details such as persistence, external integrations, and caching.
- Exclude business rules and use-case logic.

---

## Scope
- Repository implementations
- External API/client integrations
- Cache implementations
- Mapping/translation logic

---

## Principles
- Implement application/domain interfaces.
- Encapsulate external dependencies.
- Isolate external dependencies in tests.

---

## Prohibitions
- Including domain rules
- Including use-case logic
- Designing so the domain references infrastructure

---

## Deliverables
- Infrastructure implementation code
- Related tests
- Mapping/translation code
