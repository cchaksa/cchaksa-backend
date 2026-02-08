# Global

This document defines the standards Codex must follow when working in the global layer.

---

## Purpose
- Apply security, logging, exception handling, and common configuration consistently.
- Modularize cross-cutting concerns across layers.

---

## Scope
- Security configuration and auth/authz support
- Logging/tracing configuration
- Exception handling and common response structures
- Shared configuration and utilities

---

## Principles
- Do not violate other layer rules.
- Separate common functions for reuse.
- Consider environment-specific configuration when changing settings.

---

## Prohibitions
- Including domain rules
- Including use-case logic
- Exposing infrastructure details indiscriminately

---

## Deliverables
- Global configuration code
- Security/exception/logging setup
- Related tests
