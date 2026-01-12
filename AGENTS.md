# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java` contains the Spring Boot application (`com.chukchuk.haksa`) organized by domain (`domain`, `application`, `infrastructure`, `global`).
- `src/main/resources` holds configuration (`application-*.yml`), logging (`logback-spring.xml`), and API docs (`public/openapi.yaml`).
- `src/test/java` contains JUnit tests (e.g., `*Tests`).
- `docker/` includes local observability configs (Loki/Promtail).

## Architecture Overview
- `domain`: core business models, repositories, and services (entities + rules).
- `application`: use-case orchestration and API-facing DTOs/wrappers.
- `infrastructure`: external integrations (portal client, cache, mappers).
- `global`: cross-cutting concerns (security, logging, error handling, config).
- Dependency direction: `application` and `domain` should not depend on `infrastructure`; wire integrations via interfaces in `domain` (see `domain/portal/PortalCredentialStore`).
- Controllers should call `application` or `domain` services, not infrastructure classes directly.

## Build, Test, and Development Commands
- `./gradlew build` runs compile + tests and produces the build artifacts.
- `./gradlew test` runs JUnit tests via the Spring Boot test starter.
- `./gradlew bootRun` starts the app with the default profile.
- `./gradlew clean` clears generated build outputs.

## Coding Style & Naming Conventions
- Java 17, Spring Boot 3.2.x; keep 4-space indentation and standard Java formatting.
- Package naming follows `com.chukchuk.haksa.<layer>.<feature>`.
- Class names in `UpperCamelCase`, methods/fields in `lowerCamelCase`, constants in `UPPER_SNAKE_CASE`.
- Lombok is used; prefer explicit annotations over custom boilerplate.

## Testing Guidelines
- Frameworks: JUnit 5 + Spring Boot test starter.
- Test classes follow `*Tests` naming (see `src/test/java`).
- Run all tests with `./gradlew test`; add unit tests near the impacted domain or service package.

## Commit & Pull Request Guidelines
- Commit messages follow the convention in `README.md` (e.g., `feat: add login flow`, `fix: handle null token`).
- Types include `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, etc., and `!BREAKING CHANGE!` when required.
- PRs should include a clear description, linked issues (if any), and test results or reproduction steps.
- Add screenshots or API examples when changing user-facing behavior or endpoints.

## Configuration & Security Notes
- Use `application-local.yml` for local overrides; avoid committing secrets.
- External services (PostgreSQL, Redis, OIDC) should be configured via environment-specific YAML files.
