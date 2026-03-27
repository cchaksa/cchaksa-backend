# Repository Guidelines

## Technology Stack
- Java 17 (Gradle toolchain)
- Spring Boot 3.2.5
- Spring Web (MVC) and Spring WebFlux
- Spring Security with JWT (jjwt)
- Spring Data JPA (Hibernate)
- PostgreSQL
- Caffeine (local cache)
- Hibernate Validator / Jakarta Validation
- OpenAPI via springdoc
- Observability: Actuator, Sentry, Micrometer + OpenTelemetry
- Logging: Logback with logstash encoder

## Package Structure
- Base package: `com.chukchuk.haksa`
- `application`: use-case orchestration and application services
- `domain`: core models, services, controllers, repositories, wrappers, DTOs
- `infrastructure`: portal clients, cache, mappers, repositories, DTOs
- `global`: config, security, logging, exceptions, common response
- Resources: `src/main/resources` with `application-*.yml`, `logback-spring.xml`, and `public/openapi.yaml`

## Test Structure
- Tests live under `src/test/java`
- JUnit 5 via Spring Boot test starter
- Example test location: `src/test/java/com/chukchuk/haksa/global/security/cache/AuthTokenCacheTests.java`
- `tasks.test` uses JUnit Platform

## Implicit Development Rules (Observed)
- Java formatting uses 4-space indentation
- Configuration is environment-specific via `application-local.yml`, `application-dev.yml`, `application-prod.yml`
- Redis auto-configuration is excluded in `application.yml` (local cache is default)
- API documentation is served from `src/main/resources/public/openapi.yaml`
- Project-local Codex skills are managed under `codex/skills`
- Before starting work, read all files under `codex` and reflect their rules during execution
- Primary intent routing uses these task skills:
  - `FEATURE_DEVELOPMENT.md`
  - `REFACTORING.md`
  - `BUG_FIX.md`
  - `ISSUE_ANALYSIS.md`
- 모든 Context 산출물은 `docs/specs/<YYYYMMDD-slug>/` 아래 Spec Kit 하이브리드 구조(Standard 또는 Lite)를 따른다.
- If the repository guidelines change or new facts are discovered during work, update `AGENTS.md` accordingly

## Architecture Style
- Layered architecture with package boundaries: `domain` → `application` → `infrastructure` and shared `global`
- Spring Boot application entry point at `com.chukchuk.haksa.ChukchukHaksaApplication`
- Environment profiles: `local`, `dev`, `prod`

## Policy Layer

### Spec Bundle Rules
- Standard 경로: `spec.md`, `clarify.md`, `plan.md`, `tasks.md`를 모두 작성하고 Phase 1 종료 전에 승인받는다.
- Lite 경로: Scope < 1 day & 외부 API/계약 수정 없음 & 도메인 영향이 제한적일 때만 허용하며 `spec-lite.md`만 작성한다.
- Lite로 시작했다가 조건을 벗어나면 즉시 Standard 구조로 승격해 네 개 파일을 채운다.
- 브랜치명과 `docs/specs` 폴더명은 1:1 매핑한다(`YYYYMMDD-slug`).

### 1. Absolute Rules (Non-negotiable)
- Do not commit with failing tests.
- Do not change existing business logic without explicit context.
- Do not change public APIs exposed externally without a clear reason.
- Do not add new dependencies without a clear reason.
- Do not introduce changes that break transactional integrity.

### 2. Priorities (When Trade-offs Conflict)
- Safety > Correctness > Readability > Performance > Abstraction
- Prefer minimal change over broad refactors.
- Prefer explicit, readable code over clever code.
- Prefer consistency with existing style over introducing new patterns.

### 3. Future-Facing Rules (Direction, Not Hard Requirements Yet)
- When modifying code without tests, add tests when possible.
- Move business logic gradually into the domain layer.
- Gradually reduce transaction scope.
- Split commits by meaningful units (feature, refactor, test).
