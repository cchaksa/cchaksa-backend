# AGENTS.md

척척학사 백엔드에서 Codex와 다른 코딩 에이전트가 지켜야 할 저장소 규칙입니다.

## Project Context
- Java 17, Spring Boot 3.2.5, Gradle, PostgreSQL, Flyway를 사용합니다.
- 기본 패키지는 `com.chukchuk.haksa`이며 `domain`, `application`, `infrastructure`, `global` 구조를 사용합니다.

## Agent Instruction Policy
- `AGENTS.md`만 프로젝트 에이전트 규칙으로 사용합니다.
- 작업별 결정은 이슈, PR 또는 `docs/tasks/{이슈 번호}/`에 둡니다.
- 사용자가 한국어로 작성하면 한국어로 응답하고, 한국어 문장은 `.`, `?`, `!`로 끝냅니다.

## Documentation Rules
- `README.md`는 프로젝트 진입점, `CONTRIBUTING.md`는 사람용 협업 규칙, Wiki는 개발·운영 상세 가이드와 ADR을 담당합니다.
- Wiki는 별도 `https://github.com/cchaksa/cchaksa-backend.wiki.git` 저장소의 `master` 브랜치에서 관리합니다.
- 공개 API, 인증, DB 스키마, 도메인 규칙, 아키텍처, 배포, 운영, 장애 대응 변경은 Wiki 갱신 여부를 확인합니다.

## Workflow
- 작업 전에 GitHub 이슈 번호를 확인합니다.
- 설계 판단이 필요하면 `docs/tasks/{이슈 번호}/design.md`, 여러 단계 계획이 필요하면 `docs/tasks/{이슈 번호}/plan.md`를 사용합니다.
- 기존 `docs/specs`, `docs/context`, `checklist.md`, `context-notes.md`는 과거 기록으로만 유지합니다.
- 새 작업에는 기존 Spec Kit 파일인 `spec-lite.md`, `spec.md`, `clarify.md`, `tasks.md`, `checklist.md`, `context-notes.md`를 만들지 않습니다.

## Branch Rules
- `feat/{이슈 번호}`는 `dev`에서 생성해 `dev`로 병합합니다.
- `fix/{이슈 번호}`는 대상 `release/v{버전}`에서 생성해 같은 release 브랜치로 병합합니다.
- `release/v{버전}`은 `dev`에서 생성하며 QA fix를 포함해 `main`으로 병합하고 운영 배포한 뒤 `main`을 `dev`에 역병합합니다.
- `hotfix/{이슈 번호}`는 `main`에서 생성해 반드시 `main`에 먼저 병합하고 운영 배포한 뒤 `main`을 `dev`와 진행 중인 `release/*` 브랜치에 역병합합니다.

## Release Automation
- 운영 배포는 `main`에서 선행 0이나 prerelease·build suffix가 없는 최종 `MAJOR.MINOR.PATCH`를 입력해 실행합니다.
- Alias 검증 후 Actions Summary와 deployment metadata artifact를 먼저 남기고 `be-v{버전}` annotated tag와 GitHub Release를 생성합니다.
- 태그 변경, rollback, MAJOR/MINOR 결정은 사용자 확인 없이 수행하지 않습니다.

## Development Rules
- 실제 파일과 호출부를 확인하고 요청 범위만 변경합니다.
- 설정은 `application-local.yml`, `application-dev.yml`, `application-prod.yml`로 환경별 관리합니다.
- Redis 자동 구성은 `application.yml`에서 제외하며 local cache를 기본으로 사용합니다.
- 공개 API 변경은 Springdoc annotation·configuration과 계약 테스트를 함께 갱신하고 실행 중인 애플리케이션의 `/v3/api-docs`를 검증합니다.
- DB DDL 변경은 새 Flyway migration으로 남기고 적용된 migration은 수정하지 않습니다.
- Flyway migration 파일명은 현재 마지막 version 다음 번호를 사용하며, 예를 들어 `V4__add_xxx_column.sql` 형식을 사용합니다.
- prod Flyway migration은 현재 운영 중인 이전 Lambda 코드와 backward compatible해야 합니다.
- Migration 성공 뒤 테스트·빌드·Lambda 게시가 실패해 이전 Alias가 계속 트래픽을 처리하더라도 새 schema와 호환돼야 합니다.
- 적용된 migration은 수정하거나 자동 rollback하지 않으며, 보정은 다음 version의 forward migration으로 수행합니다.

## Testing And Verification
- 코드·설정·배포 변경 후 `./gradlew test --stacktrace --no-daemon`을 실행합니다.
- 문서만 변경하면 링크, `git diff --check`, 변경 범위를 검토합니다.
- 최종 응답에는 실제로 실행한 정확한 검사와 결과, 남은 위험을 포함합니다.

## Git
- 커밋은 `{이슈 번호} {type}: {한국어 메시지}` 형식을 사용합니다.
- 사용자 변경을 보존하고 dirty checkout에서는 격리 worktree를 우선합니다.

## Pull Requests
- PR 생성 시 작성자 본인을 Assignee로 지정합니다.
