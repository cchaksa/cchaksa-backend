# Domain

This document defines the standards Codex must follow when working in the domain layer.

---

## Purpose
- Express business rules and core models independent of technical details.
- Allow domain rule changes without being coupled to infrastructure changes.

---

## Scope
- Domain models, domain services, domain rules
- Domain-specific DTOs and value objects
- Domain-specific exceptions

---

## Principles
- Encode rules in code; comments are only supplementary.
- Fix domain rules by tests first.
- Any domain rule change must be grounded in Context.

---

## Prohibitions
- Direct references to infrastructure details (DB, external APIs, frameworks)
- Application use-case logic inside domain
- Controller or web-layer logic inside domain

---

## Deliverables
- Model/service code
- Domain tests
- Domain-specific exception definitions
