# Contributing Guide

척척학사 백엔드 개발 협업 규칙입니다.

## 그라운드 룰

- 작업 범위와 기존 변경을 먼저 확인하고 요청 범위만 수정합니다.
- 공개 API, 인증, DB 스키마, 도메인 규칙, 아키텍처, 배포, 운영, 장애 대응이 바뀌면 별도 `https://github.com/cchaksa/cchaksa-backend.wiki.git` 저장소의 `master` 브랜치 Wiki 갱신 여부를 확인합니다.
- 실제로 수행한 검증 결과와 남은 위험을 PR 또는 작업 완료 보고에 남깁니다.

## 이슈와 작업 문서

- GitHub 이슈 번호를 작업 식별자로 사용합니다.
- 설계 판단이 필요하면 `docs/tasks/{GitHub 이슈 번호}/design.md`를, 여러 단계 계획이나 인수인계가 필요하면 `docs/tasks/{GitHub 이슈 번호}/plan.md`를 작성합니다.
- 단순하거나 범위가 명확한 작업은 별도 작업 문서를 만들지 않습니다.
- 기존 `docs/specs`와 `docs/context`는 과거 기록으로만 참고하며 일괄 이동하거나 다시 작성하지 않습니다.
- 새 작업에는 기존 Spec Kit 파일인 `spec-lite.md`, `spec.md`, `clarify.md`, `tasks.md`, `checklist.md`, `context-notes.md`를 만들지 않습니다.

## 브랜치 전략

- 일반 기능 브랜치는 `feat/{이슈 번호}`이며 `dev`에서 시작해 `dev`로 병합합니다.
- 릴리즈 QA 수정은 대상 `release/v{버전}`에서 `fix/{이슈 번호}`를 생성해 같은 release 브랜치로 병합합니다.
- `release/v{버전}`은 `dev`에서 시작하며 QA fix를 포함해 `main`으로 병합하고 운영 배포한 뒤 `main`을 `dev`에 역병합합니다.
- 운영 긴급 수정은 `main`에서 `hotfix/{이슈 번호}`를 생성해 반드시 `main`에 먼저 병합하고 운영 배포한 뒤 `main`을 `dev`와 진행 중인 `release/*` 브랜치에 역병합합니다.

## 버전과 릴리즈

- 신규 기능은 MINOR, 운영 버그 수정은 PATCH, 호환되지 않는 변경은 MAJOR를 증가시킵니다.
- 운영 배포는 `main`에서 선행 0이나 prerelease·build suffix가 없는 최종 `MAJOR.MINOR.PATCH` 버전을 입력해 실행합니다.
- Alias 검증 후 Actions Summary와 deployment metadata artifact를 먼저 남기고 `be-v{버전}` annotated tag와 GitHub Release를 생성합니다.
- 이미 게시한 태그는 삭제하거나 다른 커밋으로 이동하지 않으며, rollback과 MAJOR/MINOR 결정은 사용자 확인 없이 수행하지 않습니다.

## 코드와 데이터베이스

- Java 17과 Spring Boot 3.2.5를 사용하며 기본 패키지는 `com.chukchuk.haksa`입니다.
- 설정은 `application-local.yml`, `application-dev.yml`, `application-prod.yml`로 관리하고 Redis 자동 구성은 제외하며 local cache를 기본으로 사용합니다.
- 공개 API 변경은 Springdoc annotation·configuration과 계약 테스트를 함께 갱신하고 실행 중인 애플리케이션의 `/v3/api-docs`를 검증합니다.
- DB DDL 변경은 새 Flyway migration으로 남기고 적용된 migration은 수정하지 않습니다.
- Flyway migration 파일명은 현재 마지막 version 다음 번호의 `V{번호}__설명.sql` 형식을 사용합니다.
- prod Flyway migration은 현재 운영 중인 이전 Lambda 코드와 backward compatible해야 합니다.
- Migration 성공 뒤 테스트·빌드·Lambda 게시가 실패해 이전 Alias가 계속 트래픽을 처리하더라도 새 schema와 호환돼야 합니다.
- 적용된 migration은 수정하거나 자동 rollback하지 않으며, 보정은 다음 version의 forward migration으로 수행합니다.

## 테스트와 검증

- 최소 애플리케이션 검증은 `./gradlew test --stacktrace --no-daemon`입니다.
- 문서만 변경하면 링크, `git diff --check`, 변경 범위를 검토합니다.
- 검증에 실패하거나 실행하지 못했으면 이유와 영향을 명확히 기록합니다.

## 커밋 컨벤션

- 커밋 형식은 `{이슈 번호} {type}: {한국어 메시지}`입니다.
- 하나의 커밋은 하나의 논리적 변경만 포함하고, 사용자 변경은 보존합니다.

## PR 규칙

- PR에는 연결 이슈, 변경 범위, 검증 결과, Wiki 갱신 여부와 남은 위험을 적습니다.
- PR에는 릴리즈 노트 분류 라벨을 하나 이상 붙이고, 릴리즈 노트에서 제외할 변경에는 `skip-release-notes` 라벨을 붙입니다.
- 대상 브랜치와 병합 전 검증은 브랜치 전략에 맞춥니다.

## 리뷰 코멘트 스타일

- 재현 가능한 근거와 영향 범위를 먼저 설명하고, 필요한 수정 제안을 구체적으로 남깁니다.
- 요청 범위 밖의 개선 제안은 차단 이슈와 분리해 제안합니다.
