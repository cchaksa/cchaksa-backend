# Application

This document defines the standards Codex must follow when working in the application layer.

---

## Purpose
- Orchestrate use-case execution flows.
- Define clear transaction boundaries.

---

## Scope
- Use-case services
- Transaction handling
- DTOs and interface contracts

---

## Principles
- Compose domain rules rather than duplicating them.
- Do not depend directly on infrastructure implementations.
- Access repositories through interfaces only.

---

## Prohibitions
- Redefining business rules
- Tight coupling to infrastructure details

---

## Deliverables
- Use-case service code
- Application tests
- DTOs and interface definitions
