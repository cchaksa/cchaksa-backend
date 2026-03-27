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

## Phase 1: Spec Bundle Creation
- Purpose: Fix feature requirements into the Spec Kit–style bundle before coding.
- Inputs: User request, prior specs in `docs/specs`, reference information from `AGENTS.md`.
- Output: `docs/specs/<YYYYMMDD-slug>/` containing `spec.md`, `clarify.md`, `plan.md`, `tasks.md` (or `spec-lite.md` when eligible).
- Sequence:
  1. Create the directory via `./scripts/new-spec.sh <YYYYMMDD-slug>` (or `--lite`).
  2. Populate `spec.md` fully using the template from `codex/skills/templates/SPEC.md`.
  3. Capture every question/decision in `clarify.md`.
  4. Draft architecture/test plan inside `plan.md`.
  5. Break work into executable units plus test commands inside `tasks.md`.
- Completion criteria:
  - [ ] Spec, clarify, plan, tasks are complete and reviewed (Lite: `spec-lite.md` filled + Lite conditions logged).
  - [ ] Stakeholder confirmation memo recorded in `spec.md`.
  - [ ] Tasks include the intended Gradle/test commands.

On failure:
- Do not proceed to the next phase.
- Return questions according to the questioning rules and append them to `clarify.md`.

### Phase 1.5: Lite Fast-Track
- 사용 조건: Scope < 1 day, 외부 API 계약/스키마 영향 없음, 트랜잭션 영향 미미.
- 산출물: `spec-lite.md` 하나. Lite 조건과 만료 시점(예: “테스트 케이스 3개 이하”)을 명시한다.
- 업그레이드: 조건을 벗어나면 즉시 Standard 템플릿 4개를 생성하고 기존 내용을 이관한다. Lite 상태에서는 Phase 2로 넘어갈 수 없다.
- In-flight migration: 기존 `docs/context/*.md`로 시작한 작업은 Phase 1 종료 전에 `docs/specs/<date-slug>`로 복사하고, 원본 파일에는 “Moved to ...” 스텁만 남긴다.

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
