# Workflow

This document defines the execution sequence Codex must follow when implementing features based on Context.
All phases are executed sequentially and must stop immediately on failure.

---

## Phase 0: Intent Routing
- Purpose: Select the primary execution skill from the user command before work starts.
- Inputs: User command, current codebase context.
- Output: One selected skill document:
  - `FEATURE_DEVELOPMENT.md`
  - `REFACTORING.md`
  - `BUG_FIX.md`
  - `ISSUE_ANALYSIS.md`
- Completion criteria:
  - [ ] Primary intent selected and documented in working notes
  - [ ] If ambiguous, questioning rules applied before execution
  - [ ] Selected skill guardrails identified

On failure:
- Do not proceed to the next phase.

---

## Phase 1: Context Creation
- Purpose: Fix feature requirements into an executable Context.
- Inputs: User request, existing Context records, reference information from `AGENTS.md`.
- Output: `docs/context/<feature>.md` or an equivalent Context artifact.
- Completion criteria:
  - [ ] All Context template items completed
  - [ ] Stakeholder confirmation memo recorded
  - [ ] Context change ready for commit

On failure:
- Do not proceed to the next phase.
- Return questions according to the questioning rules.

---

## Phase 2: Domain
- Purpose: Implement pure domain rules to define core models and service contracts.
- Input: Approved Context.
- Output:
  - `src/main/java/.../model`
  - `src/test/java/.../model`
  - Domain-specific exceptions
- Completion criteria:
  - [ ] Domain tests written first via TDD
  - [ ] `./gradlew test` (domain scope) passes
  - [ ] No layer rule violations

On failure:
- Stop immediately.

---

## Phase 3: Application
- Purpose: Implement use-case orchestration and transaction boundaries.
- Inputs: Context, Domain outputs.
- Output:
  - `src/main/java/.../application`
  - `src/test/java/.../application`
  - DTOs and interface contracts
- Completion criteria:
  - [ ] Depends only on Domain
  - [ ] Access via repository interfaces only
  - [ ] `./gradlew test` passes

On failure:
- Stop immediately.

---

## Phase 4: Infrastructure

- Purpose:
  - Handle technical details such as persistence, external integrations, and cache.
  - Do not include business rules or use-case logic.

- Inputs:
  - Context
  - Domain models
  - Repository interfaces defined in Application

- Output:
  - `src/main/java/.../repository`
    - Repository implementations (JPA, etc.)
    - External API clients
    - Cache implementations
  - Domain ↔ Entity mapping code
  - Corresponding tests

- Completion criteria:
  - [ ] Infrastructure depends only on Domain/Application
  - [ ] Domain does not reference Infrastructure
  - [ ] No business logic included
  - [ ] Tests pass with external dependencies isolated
  - [ ] `./gradlew test` fully passes

On failure:
- Stop immediately.

---

## Phase 5: Global / Config
- Purpose: Apply security, logging, exception handling, and common configuration.
- Inputs: Context, existing Global configuration.
- Output:
  - `src/main/java/.../global`
  - `src/main/resources` configuration
- Completion criteria:
  - [ ] No layer rule violations
  - [ ] `./gradlew build` passes

On failure:
- Stop immediately.

---

## Phase 6: API / Controller
- Purpose: Complete the API layer that exposes Application services.
- Inputs: Context, Application services, Global configuration.
- Output:
  - `src/main/java/.../domain/*/controller`
  - OpenAPI documentation
  - API tests
- Completion criteria:
  - [ ] Controller tests pass
  - [ ] API documentation is up to date
  - [ ] `./gradlew test` passes

On failure:
- Stop immediately.

---

## Phase 7: Commit & Pull Request
- Purpose: Fix all changes into a single execution unit.
- Inputs: Outputs from Phases 1–6.
- Output:
  - Git commit
  - Pull Request
- Completion criteria:
  - [ ] Commit message includes Context ID
  - [ ] PR includes Context summary, change list, and test results
  - [ ] One PR per Context
