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

## Documentation Maintenance
- Keep `README.md` as the project entry point and the GitHub Wiki as the detailed developer and operations guide.
- The Wiki is the separate `https://github.com/cchaksa/cchaksa-backend.wiki.git` repository and uses `master` as its default branch.
- Before completing a task, decide whether changes to public APIs, authentication, database schema, domain rules, architecture, deployment, operations, or incident response require a Wiki update.
- Add or supersede an ADR when an architecture decision, its constraints, or its operational consequences change.
- Internal refactors and test-only changes normally do not require a Wiki update. State why no Wiki update was needed in the final response.
- When a Wiki update is required, update the separate Wiki repository, verify its links and sensitive-data safety, push it, and confirm the public page renders.
- Follow the human-facing documentation and commit procedure in the Wiki `Development Guide`; do not duplicate its long-form content here.

## Communication
- When the user writes in Korean, respond in Korean unless the task requires another language.
- Korean sentences should end with `.`, `?`, or `!`, not a closing colon.

## Development Rules
- Inspect the actual files and nearby call sites before editing.
- Keep changes surgical and directly tied to the request.
- Prefer the smallest explicit solution that preserves existing behavior.
- Do not change business logic, public API contracts, dependencies, or transaction boundaries without clear context.
- Match existing Java formatting with 4-space indentation.
- Keep configuration environment-specific through `application-local.yml`, `application-dev.yml`, and `application-prod.yml`.
- Redis auto-configuration is excluded in `application.yml`; local cache is the default.
- Keep API documentation in `src/main/resources/public/openapi.yaml` aligned with externally visible API changes.

## Database Migration Rules
- DB DDL 변경이 필요한 코드 변경은 반드시 `src/main/resources/db/migration` 아래 Flyway SQL 파일로 남긴다.
- 엔티티, 컬럼, 테이블, 인덱스, 제약 조건 변경은 Java 코드만 수정해서 끝내지 않는다.
- Migration 파일명은 현재 마지막 version 다음 번호를 사용한다. 예: `V4__add_xxx_column.sql`.
- 이미 dev/prod 중 하나라도 적용된 migration 파일은 수정하지 않는다.
- 적용된 migration의 보정이 필요하면 기존 파일을 고치지 않고 다음 version의 새 migration 파일을 추가한다.
- Hibernate `ddl-auto`는 dev/prod에서 schema 변경 수단으로 사용하지 않는다. DB 변경은 Flyway migration을 통해 수행한다.

## Planning And Specs
- Use `docs/specs/<YYYYMMDD-slug>/` only when the work affects one or more of these areas: domain rules, public API contracts, database schema, security/auth behavior, transactions, deployment, rollback, or cross-module architecture.
- Do not create a spec for small mechanical changes, narrow bug fixes, test-only changes, documentation-only changes, or single-file config updates unless the user asks for one.
- Prefer a single `spec-lite.md` for work expected to fit within one day.
- Use the full bundle, `spec.md`, `clarify.md`, `plan.md`, and `tasks.md`, only when requirements are unclear, the work spans multiple modules, or the rollout/rollback path needs explicit tracking.
- If the user says to skip specs, use a concise conversation plan instead. When the change affects database schema, public API contracts, or security/auth behavior, briefly confirm whether they still want no spec before proceeding without one.
- `./scripts/new-spec.sh <YYYYMMDD-slug> [--lite]` can create the expected spec files.

## Testing
- Tests live under `src/test/java`.
- JUnit 5 is provided by the Spring Boot test starter.
- `tasks.test` uses JUnit Platform.
- If code changes, run the smallest relevant test first, then broader checks when risk is high.
- If public API, configuration, security, persistence, or integration behavior changes, run `./gradlew test` unless there is a concrete blocker.
- Final replies after code or docs changes must include the exact checks run, their result, and any remaining risk.
- Do not commit with failing tests. If verification cannot run, state the exact reason.

## Git
- Work branches map to a GitHub issue and use `feat/{github-issue-number}`. Ask for the issue number before starting if it is missing.
- If the current checkout has unrelated dirty changes and the task needs a new branch, prefer an isolated git worktree.
- Keep unrelated user changes intact. Do not revert, overwrite, or reformat them.
- Split commits by meaningful unit when changes are separable.
- Commit messages are written in Korean and start with the branch issue number.
- Commit message format: `{issue-number} {type}: {message}`.
- Example: `236 chore: 프로젝트 로컬 에이전트 규약 제거`.
