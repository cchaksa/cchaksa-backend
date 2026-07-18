# 백엔드 버전 및 릴리즈 관리 설계

## 연결 이슈

- GitHub Issue [#306](https://github.com/cchaksa/cchaksa-backend/issues/306)

## 목표

척척학사 백엔드의 정식 릴리즈를 Semantic Versioning으로 식별하고, 릴리즈 브랜치부터 실제 Lambda alias까지 하나의 추적 가능한 배포 기록으로 연결한다. 저장소 문서도 Landit BE처럼 에이전트 규칙, 사람용 협업 규칙, 작업별 설계를 분리한다.

## 릴리즈 설계

### 브랜치와 버전

- 일반 개발은 기존처럼 `feat/{이슈 번호}`에서 진행하고 `dev`로 병합한다.
- 정식 릴리즈는 `dev`에서 `release/v{MAJOR.MINOR.PATCH}`를 생성해 범위를 동결하고 최종 검증 후 `main`으로 병합한다.
- 릴리즈 QA 수정은 대상 release 브랜치에서 `fix/{이슈 번호}`를 생성하고 같은 release 브랜치로 병합한다.
- QA fix를 포함한 release 브랜치를 `main`으로 병합해 운영 배포한 뒤 `main`을 `dev`에 역병합한다.
- 운영 긴급 수정은 `main`에서 `hotfix/{이슈 번호}`를 생성해 반드시 `main`에 먼저 병합하고 운영 배포한 뒤 `main`을 `dev`와 진행 중인 `release/*` 브랜치에 역병합한다.
- 신규 기능은 MINOR, 운영 버그 수정은 PATCH, 호환되지 않는 변경은 MAJOR를 증가시킨다.
- 최초 적용 버전을 포함한 MAJOR/MINOR 결정은 자동화하지 않고 실제 릴리즈 시 사용자가 승인한다.
- workflow 입력은 각 숫자가 `0` 또는 선행 0 없는 양의 정수인 최종 `MAJOR.MINOR.PATCH`만 허용하며 prerelease와 build suffix는 허용하지 않는다.

### 프로덕션 배포

1. 사용자가 `main`에서 `Deploy to Prod Lambda` workflow를 수동 실행하며 선행 0과 suffix가 없는 최종 `MAJOR.MINOR.PATCH` 버전을 입력한다.
2. preflight job이 실행 브랜치, 버전 형식, 기존 `be-v{버전}` 태그 중복을 배포 전에 검증한다.
3. workflow 실행을 시작한 `main` 커밋 SHA를 Flyway migration과 Lambda 빌드의 동일한 기준으로 사용한다.
4. 기존 순서대로 테스트, Lambda zip 업로드, 새 Lambda version publish, alias 전환 및 alias version 검증을 수행한다.
5. Alias 검증까지 성공한 후 Actions Summary를 작성하고 제품 버전, 릴리즈 태그, 커밋 SHA, Lambda published version과 alias version을 담은 deployment metadata artifact를 업로드한다.
6. 추적 증거가 보존된 뒤 같은 커밋에 `be-v{버전}` annotated tag를 push하고 GitHub Release를 생성한다. Release 본문에도 제품 버전, 커밋 SHA, Lambda published version과 alias version을 기록한다.

prod Flyway migration은 현재 운영 중인 이전 Lambda 코드와 backward compatible해야 한다. Migration 성공 후 테스트·빌드·Lambda 게시가 실패하면 이전 Alias가 계속 트래픽을 처리하므로 새 schema에서도 이전 코드가 정상 동작해야 한다. 적용된 migration을 수정하거나 자동 rollback하지 않으며, 보정은 다음 version의 forward migration으로 수행한다.

### 실패와 복구

- Alias 검증 전에 실패하면 배포 실패이며 태그와 GitHub Release가 생성되지 않는다. Migration은 이미 적용됐을 수 있으므로 이전 코드와 새 schema의 호환성을 확인한다.
- Alias 검증 성공 뒤 태그 또는 GitHub Release 게시가 실패하면 Lambda 배포는 성공했지만 릴리즈 기록 게시가 실패한 부분 성공이다.
- 태그 push 후 GitHub Release 생성만 실패한 부분 성공은 자동으로 태그를 삭제하거나 이동하지 않는다. 먼저 deployment metadata artifact로 커밋 SHA와 Lambda published·alias version을 확인한 뒤 같은 태그에 GitHub Release만 생성한다.
- 이미 게시한 태그는 삭제하거나 다른 커밋으로 이동하지 않는다.
- rollback, 과거 배포 태그 복원, MAJOR/MINOR 자동 결정은 이번 작업에 포함하지 않는다.

## 문서 체계 전환

### 문서별 역할

- `README.md`는 프로젝트 진입점으로 유지한다.
- `AGENTS.md`는 에이전트가 반드시 지켜야 하는 짧고 실행 가능한 저장소 규칙만 담는다.
- `CONTRIBUTING.md`는 이슈, 브랜치, 릴리즈, 코드, 테스트, 커밋, PR과 리뷰에 관한 사람용 협업 규칙을 담는다.
- GitHub Wiki는 개발·운영 상세 가이드와 ADR을 관리한다.
- 작업별 문서는 필요한 경우에만 `docs/tasks/{GitHub 이슈 번호}/` 아래에 둔다.

### 작업 문서 규칙

- 요구사항이 모호하거나 설계 판단이 필요하면 `design.md`를 작성한다.
- 구현이 여러 단계이거나 인수인계가 필요하면 `plan.md`를 작성한다.
- 승인된 `design.md`와 `plan.md`를 작업의 단일 기준으로 사용한다.
- 단순하거나 범위가 명확한 작업은 별도 작업 문서를 만들지 않는다.
- 모든 새 작업은 GitHub 이슈 번호를 사용한다.
- 새 작업에는 `spec-lite.md`, `spec.md`, `clarify.md`, `tasks.md`, `checklist.md`, `context-notes.md`를 만들지 않는다.

### 기존 문서 처리

- 기존 `docs/specs`와 `docs/context` 문서는 과거 기록으로 유지하고 일괄 이동하거나 다시 작성하지 않는다.
- `docs/specs/README.md`는 새 작업에 사용하지 않는 과거 문서라는 안내만 남긴다.
- 기존 Spec Kit 문서를 만드는 `scripts/new-spec.sh`는 제거한다.
- 이 이슈의 설계 문서는 새 기준을 바로 적용해 `docs/tasks/306/design.md`에 둔다.

## 변경 범위

- `.github/workflows/deploy-prod-lambda.yml`
- `AGENTS.md`
- `CONTRIBUTING.md`
- `docs/specs/README.md`
- `docs/tasks/306/design.md`
- `scripts/new-spec.sh`

dev 배포 workflow, 애플리케이션 코드, 공개 API, DB 스키마와 Gradle 프로젝트 버전은 변경하지 않는다. 릴리즈 버전의 기준은 운영 workflow 입력과 성공 후 생성되는 Git 태그 및 GitHub Release다.

## 검증

- YAML parser로 변경 workflow의 문법을 검증한다.
- 유효 버전, 잘못된 버전, 중복 태그에 대한 preflight shell 로직을 로컬 임시 Git 저장소에서 검증한다.
- 변경된 Markdown 링크와 문서 간 규칙이 일치하는지 확인한다.
- `git diff --check`를 통과한다.
- `./gradlew test --stacktrace --no-daemon`을 통과한다.

## 완료 기준

- 정식 릴리즈 브랜치와 fix/hotfix 흐름이 저장소 규칙에 명시된다.
- 프로덕션 workflow는 `main`과 미사용 Semantic Version을 배포 전에 검증한다.
- 실제 배포에 사용한 단일 커밋 SHA가 migration, Lambda artifact, 태그와 GitHub Release에 일관되게 연결된다.
- Lambda Alias 검증 후 Summary와 metadata artifact가 먼저 보존되고 `be-v{버전}` annotated tag와 GitHub Release가 생성된다.
- 새 작업 문서가 `docs/tasks/{이슈 번호}/design.md`와 `plan.md` 중심으로 관리된다.
- 기존 프로젝트 문서는 과거 기록으로 보존되고 dev 배포와 애플리케이션 동작에는 변화가 없다.
