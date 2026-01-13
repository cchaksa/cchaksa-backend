# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java` contains the Spring Boot app (`com.chukchuk.haksa`) organized as `domain`, `application`, `infrastructure`, `global`.
- `src/main/resources` holds config (`application-*.yml`), logging (`logback-spring.xml`), and API docs (`public/openapi.yaml`).
- `src/test/java` holds JUnit tests (e.g., `*Tests`); `docker/` has local observability configs.

## Architecture Overview
- `domain`: entities + business rules; `application`: use-case orchestration; `infrastructure`: portal client/cache/mappers; `global`: security/logging/error/config.
- Keep dependencies pointing inward; use interfaces in `domain` (e.g., `domain/portal/PortalCredentialStore`).
- Portal sync flow: credentials → scrape → initialize/refresh → persistence.

## Build, Test, and Development Commands
- `./gradlew build` (compile + tests), `./gradlew test`, `./gradlew bootRun`, `./gradlew clean`.

## Coding Style & Naming Conventions
- Java 17, Spring Boot 3.2.x; 4-space indentation, standard Java formatting.
- Packages: `com.chukchuk.haksa.<layer>.<feature>`; classes `UpperCamelCase`, fields `lowerCamelCase`.
- Lombok is used; prefer explicit annotations.

## Testing Guidelines
- JUnit 5 + Spring Boot test starter; tests named `*Tests`.
- Add unit tests near the impacted service/domain package; run with `./gradlew test`.
- After all changes, run `./gradlew test` to validate the full suite before sending PRs.

## Commit & Pull Request Guidelines
- Follow `README.md` commit types (`feat`, `fix`, `refactor`, `docs`, `test`, `chore`, etc.).
- PRs include summary, linked issues, and test results; add API examples for endpoint changes.

## Configuration, Auth, and Cache Notes
- Use `application-local.yml` for overrides; avoid committing secrets.
- Cache strategy: `cache.type=local|redis`, `portal.credential.store=local|redis`.
- Auth cache: `AuthTokenCache` stores token → `UserDetails` for access-token TTL; evict by user on deletion.

## Domain Rules & Priorities
- Graduation requirement analysis is domain-driven; missing or inconsistent data is treated as a domain exception, not a technical error.
- Portal sync success does not guarantee graduation analysis success (e.g., transfer students, missing requirements).
- Domain correctness takes priority over API convenience or client expectations.

## Refactoring Guardrails
- Do not merge domain and infrastructure models.
- Do not bypass application services to simplify flows.
- Avoid introducing global transactional boundaries without explicit justification.

## Transaction Boundaries
- Transactions are managed at the application service level, not in controllers or domain models.

## Agent Notes (type/*)
- On branch `type/*`, commit messages start with `* ` and are written in Korean.
  - type: feat, refactor, fix
