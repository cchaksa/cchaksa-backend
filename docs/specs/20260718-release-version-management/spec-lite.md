# 백엔드 버전 및 릴리즈 관리 정책

## 연결 이슈

- GitHub Issue [#306](https://github.com/cchaksa/cchaksa-backend/issues/306)

## 목표

척척학사 백엔드의 정식 릴리즈를 Semantic Versioning으로 식별하고, 릴리즈 브랜치부터 실제 Lambda alias까지 하나의 추적 가능한 배포 기록으로 연결한다.

## 설계

### 브랜치와 버전

- 일반 개발은 기존처럼 `feat/{이슈 번호}`에서 진행하고 `dev`로 병합한다.
- 정식 릴리즈는 `dev`에서 `release/v{MAJOR.MINOR.PATCH}`를 생성해 범위를 동결하고 최종 검증 후 `main`으로 병합한다.
- 릴리즈 QA 수정은 대상 release 브랜치에서 `fix/{이슈 번호}`를 생성하고 같은 release 브랜치로 병합한다.
- 운영 긴급 수정은 `main`에서 `hotfix/{이슈 번호}`를 생성하고 배포 후 `dev`와 진행 중인 release 브랜치에 반영한다.
- 신규 기능은 MINOR, 운영 버그 수정은 PATCH, 호환되지 않는 변경은 MAJOR를 증가시킨다.
- 최초 적용 버전을 포함한 MAJOR/MINOR 결정은 자동화하지 않고 실제 릴리즈 시 사용자가 승인한다.

### 프로덕션 배포

1. 사용자가 `main`에서 `Deploy to Prod Lambda` workflow를 수동 실행하며 `MAJOR.MINOR.PATCH` 버전을 입력한다.
2. preflight job이 실행 브랜치, 버전 형식, 기존 `be-v{버전}` 태그 중복을 배포 전에 검증한다.
3. workflow 실행을 시작한 `main` 커밋 SHA를 Flyway migration과 Lambda 빌드의 동일한 기준으로 사용한다.
4. 기존 순서대로 테스트, Lambda zip 업로드, 새 Lambda version publish, alias 전환 및 alias version 검증을 수행한다.
5. alias 검증까지 성공한 후 같은 커밋에 `be-v{버전}` annotated tag와 GitHub Release를 생성한다.
6. Actions summary와 배포 metadata artifact에 제품 버전, 릴리즈 태그, 커밋 SHA, Lambda published version과 alias version을 기록한다.

### 실패와 복구

- preflight, migration, 테스트, 빌드, Lambda publish 또는 alias 검증이 실패하면 태그와 GitHub Release 생성 단계에 도달하지 않는다.
- 태그 push 후 GitHub Release 생성만 실패한 부분 성공은 자동으로 태그를 삭제하거나 이동하지 않는다. 해당 태그와 workflow 실행을 확인한 뒤 GitHub Release만 복구한다.
- 이미 게시한 태그는 삭제하거나 다른 커밋으로 이동하지 않는다.
- rollback, 과거 배포 태그 복원, MAJOR/MINOR 자동 결정은 이번 작업에 포함하지 않는다.

## 변경 범위

- `.github/workflows/deploy-prod-lambda.yml`
- `AGENTS.md`
- `CONTRIBUTING.md`
- 이 spec 문서

dev 배포 workflow, 애플리케이션 코드, 공개 API, DB 스키마와 Gradle 프로젝트 버전은 변경하지 않는다. 릴리즈 버전의 기준은 운영 workflow 입력과 성공 후 생성되는 Git 태그 및 GitHub Release다.

## 검증

- YAML parser로 변경 workflow의 문법을 검증한다.
- 유효 버전, 잘못된 버전, 중복 태그에 대한 preflight shell 로직을 로컬 임시 Git 저장소에서 검증한다.
- `git diff --check`를 통과한다.
- `./gradlew test --stacktrace --no-daemon`을 통과한다.

## 완료 기준

- 정식 릴리즈 브랜치와 fix/hotfix 흐름이 저장소 규칙에 명시된다.
- 프로덕션 workflow는 `main`과 미사용 Semantic Version을 배포 전에 검증한다.
- 실제 배포에 사용한 단일 커밋 SHA가 migration, Lambda artifact, 태그와 GitHub Release에 일관되게 연결된다.
- Lambda alias 검증 후 `be-v{버전}` annotated tag와 GitHub Release가 생성된다.
- dev 배포와 애플리케이션 동작에는 변화가 없다.
