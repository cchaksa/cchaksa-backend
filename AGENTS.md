# Repository Guidelines

## Project Facts
- Java 17 Gradle toolchain.
- Spring Boot 3.2.5.
- Spring Web MVC and WebFlux.
- Spring Security with JWT through `jjwt`.
- Spring Data JPA with PostgreSQL.
- Caffeine local cache.
- Hibernate Validator and Jakarta Validation.
- OpenAPI via springdoc.
- Observability through Actuator, Sentry, Micrometer, and OpenTelemetry.
- Logging through Logback with logstash encoder.

## Package Map
- Base package: `com.chukchuk.haksa`.
- `domain`: core models, domain services, controllers, repositories, wrappers, and DTOs.
- `application`: use-case orchestration and application services.
- `infrastructure`: portal clients, cache, mappers, repository implementations, and integration DTOs.
- `global`: configuration, security, logging, exceptions, and common response code.
- Resources live under `src/main/resources`, including `application-*.yml`, `logback-spring.xml`, and `public/openapi.yaml`.

## Agent Instruction Policy
- `AGENTS.md` is the only project-level AI Agent instruction source.
- Do not add or maintain separate project-local agent rule files, including `codex/skills`, `CLAUDE.md`, `GEMINI.md`, or tool-specific rule folders.
- Keep this file short and durable. Put task-specific decisions in the relevant issue, PR, spec, or final response instead.
- Update `AGENTS.md` only when a durable project-wide rule changes, and call that out explicitly.

## Development Rules
- Inspect the actual files and nearby call sites before editing.
- Keep changes surgical and directly tied to the request.
- Prefer the smallest explicit solution that preserves existing behavior.
- Do not change business logic, public API contracts, dependencies, or transaction boundaries without clear context.
- Match existing Java formatting with 4-space indentation.
- Keep configuration environment-specific through `application-local.yml`, `application-dev.yml`, and `application-prod.yml`.
- Redis auto-configuration is excluded in `application.yml`; local cache is the default.
- Keep API documentation in `src/main/resources/public/openapi.yaml` aligned with externally visible API changes.

## Planning And Specs
- Use `docs/specs/<YYYYMMDD-slug>/` for non-trivial feature work, bug fixes, refactors, or operational changes that affect domain rules, public APIs, schema, transactions, deployment, or rollback.
- For small, mechanical, documentation-only, or test-only changes, a concise plan in the conversation is enough.
- Standard spec bundles use `spec.md`, `clarify.md`, `plan.md`, and `tasks.md`.
- Lite specs use `spec-lite.md` when the work is under one day, does not change external contracts, and has limited domain impact.
- If requirements are unclear, ask before editing the unclear part. Record durable decisions in the spec when one exists.
- `./scripts/new-spec.sh <YYYYMMDD-slug> [--lite]` can create the expected spec files.

## Testing
- Tests live under `src/test/java`.
- JUnit 5 is provided by the Spring Boot test starter.
- `tasks.test` uses JUnit Platform.
- If code changes, run the smallest relevant test first, then broader checks when risk is high.
- If public API, configuration, security, persistence, or integration behavior changes, run `./gradlew test` unless there is a concrete blocker.
- Do not commit with failing tests. If verification cannot run, state the exact reason.

## Git
- Work branches map to a GitHub issue and use `feat/{github-issue-number}`. Ask for the issue number before starting if it is missing.
- Keep unrelated user changes intact. Do not revert, overwrite, or reformat them.
- Split commits by meaningful unit when changes are separable.
- Commit messages are written in Korean and start with the branch issue number.
- Commit message format: `{issue-number} {type}: {message}`.
- Example: `236 chore: 프로젝트 로컬 에이전트 규약 제거`.
