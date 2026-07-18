# 백엔드 버전 및 릴리즈 관리 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 척척학사 백엔드의 문서 체계를 Landit BE 방식으로 전환하고, 검증된 프로덕션 Lambda 배포에 `be-v{버전}` 태그와 GitHub Release를 남긴다.

**Architecture:** 사람용 협업 규칙은 `CONTRIBUTING.md`, 에이전트용 강제 규칙은 `AGENTS.md`, 작업별 설계와 계획은 `docs/tasks/{이슈 번호}/`에 둔다. 프로덕션 workflow에는 배포 전 preflight job을 추가하고, workflow를 시작한 `main` 커밋을 migration·Lambda artifact·Git 태그·GitHub Release의 공통 기준으로 사용한다.

**Tech Stack:** GitHub Actions, Bash, Git, GitHub CLI, Gradle, AWS Lambda, Flyway, Markdown.

## Global Constraints

- 기존 `docs/specs`와 `docs/context` 문서는 삭제하거나 일괄 이동하지 않는다.
- dev 배포 workflow, 애플리케이션 코드, 공개 API, DB 스키마와 Gradle 프로젝트 버전은 변경하지 않는다.
- 기존 Flyway migration, 테스트, Lambda publish, alias 전환과 검증 순서를 유지한다.
- 태그 삭제·이동, 자동 rollback, 과거 태그 복원, MAJOR/MINOR 자동 결정은 구현하지 않는다.
- 새 작업 문서는 `docs/tasks/{GitHub 이슈 번호}/design.md`와 `plan.md`만 필요할 때 작성한다.
- 문서 변경과 workflow 변경은 서로 다른 커밋으로 남긴다.

---

### Task 1: 저장소 문서 체계를 Landit BE 방식으로 전환

**Files:**
- Modify: `AGENTS.md`
- Create: `CONTRIBUTING.md`
- Modify: `docs/specs/README.md`
- Delete: `scripts/new-spec.sh`
- Reference: `docs/tasks/306/design.md`

**Interfaces:**
- Consumes: GitHub 이슈 번호, `dev`/`main` 브랜치 구조, 별도 Wiki 저장소, 현재 Gradle 검증 명령.
- Produces: 에이전트용 저장소 규칙, 사람용 협업 규칙, 새 작업 문서 경로와 과거 문서 경계.

- [ ] **Step 1: 현재 규칙의 보존 항목을 확인한다**

Run:

```bash
rg -n '^## |docs/specs|docs/tasks|Wiki|Flyway|OpenAPI|feat/|commit|gradlew' AGENTS.md docs/specs/README.md
```

Expected: 프로젝트 사실, Wiki 갱신, Flyway, OpenAPI, 테스트, Git 규칙과 기존 `docs/specs` 규칙이 모두 확인된다.

- [ ] **Step 2: `AGENTS.md`를 에이전트 규칙 중심으로 다시 작성한다**

Required structure and rules:

```markdown
# AGENTS.md

척척학사 백엔드에서 Codex와 다른 코딩 에이전트가 지켜야 할 저장소 규칙입니다.

## Project Context
- Java 17, Spring Boot 3.2.5, Gradle, PostgreSQL, Flyway를 사용합니다.
- 기본 패키지는 `com.chukchuk.haksa`이며 `domain`, `application`, `infrastructure`, `global` 구조를 사용합니다.

## Agent Instruction Policy
- `AGENTS.md`만 프로젝트 에이전트 규칙으로 사용합니다.
- 작업별 결정은 이슈, PR 또는 `docs/tasks/{이슈 번호}/`에 둡니다.

## Documentation Rules
- `README.md`는 진입점, Wiki는 개발·운영 상세 가이드와 ADR을 담당합니다.
- 공개 API, 인증, DB 스키마, 도메인 규칙, 아키텍처, 배포, 운영, 장애 대응 변경은 Wiki 갱신 여부를 확인합니다.

## Workflow
- 작업 전에 GitHub 이슈 번호를 확인합니다.
- 설계 판단이 필요하면 `docs/tasks/{이슈 번호}/design.md`, 여러 단계 계획이 필요하면 `plan.md`를 사용합니다.
- 기존 `docs/specs`, `docs/context`, `checklist.md`, `context-notes.md`는 과거 기록으로만 유지합니다.

## Branch Rules
- `feat/{이슈 번호}`는 `dev`에서 생성해 `dev`로 병합합니다.
- `fix/{이슈 번호}`는 대상 `release/v{버전}`에서 생성해 같은 release 브랜치로 병합합니다.
- `release/v{버전}`은 `dev`에서 생성해 검증 후 `main`으로 병합합니다.
- `hotfix/{이슈 번호}`는 `main`에서 생성해 배포 후 `dev`와 진행 중인 release 브랜치에 반영합니다.

## Release Automation
- 운영 배포는 `main`에서 `MAJOR.MINOR.PATCH`를 입력해 실행합니다.
- 성공 후 `be-v{버전}` annotated tag와 GitHub Release를 생성합니다.
- 태그 변경, rollback, MAJOR/MINOR 결정은 사용자 확인 없이 수행하지 않습니다.

## Development Rules
- 실제 파일과 호출부를 확인하고 요청 범위만 변경합니다.
- 공개 API 변경은 `src/main/resources/public/openapi.yaml`을 함께 갱신합니다.
- DB DDL 변경은 새 Flyway migration으로 남기고 적용된 migration은 수정하지 않습니다.

## Testing And Verification
- 코드·설정·배포 변경 후 `./gradlew test --stacktrace --no-daemon`을 실행합니다.
- 문서만 변경하면 링크, `git diff --check`, 변경 범위를 검토합니다.

## Git
- 커밋은 `{이슈 번호} {type}: {한국어 메시지}` 형식을 사용합니다.
- 사용자 변경을 보존하고 dirty checkout에서는 격리 worktree를 우선합니다.
```

Preserve the existing Korean sentence-ending rule, Wiki repository URL and `master` branch, configuration profile names, local-cache rule, Flyway filename rule, and final verification evidence rule.

- [ ] **Step 3: 사람용 `CONTRIBUTING.md`를 작성한다**

Required sections:

```markdown
# Contributing Guide

척척학사 백엔드 개발 협업 규칙입니다.

## 그라운드 룰
## 이슈와 작업 문서
## 브랜치 전략
## 버전과 릴리즈
## 코드와 데이터베이스
## 테스트와 검증
## 커밋 컨벤션
## PR 규칙
## 리뷰 코멘트 스타일
```

The document must state these exact repository-specific decisions:

```text
GitHub 이슈 번호를 작업 식별자로 사용한다.
일반 기능 브랜치는 feat/{이슈 번호}이며 dev에서 시작해 dev로 병합한다.
release/v{버전}은 dev에서 시작해 main으로 병합한다.
운영 배포 성공 후 be-v{버전} 태그와 GitHub Release를 생성한다.
커밋 형식은 {이슈 번호} {type}: {한국어 메시지}다.
최소 애플리케이션 검증은 ./gradlew test --stacktrace --no-daemon이다.
```

Do not copy Landit-only rules such as Notion issue identifiers, Java 21, Spring Boot 4, Spotless, Checkstyle, `develop`, ECS or OIDC deployment.

- [ ] **Step 4: 기존 Spec Kit 진입점을 과거 기록으로 전환한다**

Replace `docs/specs/README.md` with:

```markdown
# 이전 작업 문서

이 디렉터리는 기존 Spec Kit 방식으로 작성된 작업 문서를 보존합니다.

새 작업 문서는 필요할 때만 `docs/tasks/{GitHub 이슈 번호}/design.md`와 `plan.md`로 작성합니다. 기존 파일은 과거 결정과 구현 기록을 확인할 때만 참고하며 일괄 이동하거나 다시 작성하지 않습니다.
```

Delete `scripts/new-spec.sh`. Do not delete any existing directory below `docs/specs` or `docs/context`.

- [ ] **Step 5: 문서 규칙의 일관성을 검증한다**

Run:

```bash
test -f CONTRIBUTING.md
test ! -e scripts/new-spec.sh
rg -n 'docs/tasks/\{.*이슈.*\}|release/v|be-v|GitHub Release' AGENTS.md CONTRIBUTING.md docs/tasks/306/design.md
! rg -n '새 작업.*docs/specs|new-spec\.sh|Notion|Java 21|Spring Boot 4|Spotless|Checkstyle|develop|ECS' AGENTS.md CONTRIBUTING.md docs/specs/README.md
git diff --check
```

Expected: 모든 명령이 exit code 0이며 새 규칙은 `docs/tasks`만 가리키고 Landit 전용 값은 포함하지 않는다.

- [ ] **Step 6: 문서 변경을 커밋한다**

```bash
git add AGENTS.md CONTRIBUTING.md docs/specs/README.md scripts/new-spec.sh
git commit -m "306 docs: 협업 및 작업 문서 규칙 전환"
```

---

### Task 2: 프로덕션 Lambda 릴리즈 자동화 추가

**Files:**
- Modify: `.github/workflows/deploy-prod-lambda.yml`
- Reference: `.github/workflows/flyway-migration.yml`
- Reference: `docs/tasks/306/design.md`

**Interfaces:**
- Consumes: `workflow_dispatch.inputs.version`, `github.ref`, `github.sha`, 기존 AWS 환경 변수와 Lambda alias 검증 결과.
- Produces: `release_version`, `release_tag`, `be-v{버전}` annotated tag, GitHub Release, 확장된 Actions summary와 deployment metadata artifact.

- [ ] **Step 1: workflow 입력, 권한, 동시 실행 제한과 preflight job을 추가한다**

Use this structure at the top of `.github/workflows/deploy-prod-lambda.yml`:

```yaml
name: Deploy to Prod Lambda

on:
  workflow_dispatch:
    inputs:
      version:
        description: '배포 버전. 예시 1.2.0'
        required: true
        type: string

permissions:
  contents: write

concurrency:
  group: prod-lambda-deploy
  cancel-in-progress: false

jobs:
  preflight:
    name: Validate prod release
    runs-on: ubuntu-latest
    outputs:
      release_version: ${{ steps.release.outputs.release_version }}
      release_tag: ${{ steps.release.outputs.release_tag }}

    steps:
      - name: Validate main branch
        run: |
          if [ "$GITHUB_REF" != "refs/heads/main" ]; then
            echo "Production deploy must run from main branch" >&2
            exit 1
          fi

      - name: Checkout release commit
        uses: actions/checkout@v4
        with:
          ref: ${{ github.sha }}
          fetch-depth: 0

      - name: Validate release version
        id: release
        env:
          RELEASE_VERSION: ${{ inputs.version }}
        run: |
          if [[ ! "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            echo "Version must use MAJOR.MINOR.PATCH format" >&2
            exit 1
          fi

          release_tag="be-v$RELEASE_VERSION"
          if git rev-parse -q --verify "refs/tags/$release_tag" >/dev/null; then
            echo "$release_tag already exists" >&2
            exit 1
          fi

          echo "release_version=$RELEASE_VERSION" >> "$GITHUB_OUTPUT"
          echo "release_tag=$release_tag" >> "$GITHUB_OUTPUT"
```

- [ ] **Step 2: migration과 배포가 동일한 dispatch SHA를 사용하게 한다**

Replace the job wiring with:

```yaml
  migrate:
    needs: preflight
    uses: ./.github/workflows/flyway-migration.yml
    with:
      environment: prod
      ref: ${{ github.sha }}
    secrets: inherit

  deploy:
    needs:
      - preflight
      - migrate
    runs-on: ubuntu-latest
    env:
      AWS_REGION: ${{ vars.PROD_AWS_REGION }}
      LAMBDA_FUNCTION_NAME: ${{ vars.PROD_LAMBDA_FUNCTION_NAME }}
      LAMBDA_ALIAS: ${{ vars.PROD_LAMBDA_ALIAS }}
      LAMBDA_ARTIFACT_BUCKET: ${{ vars.PROD_LAMBDA_ARTIFACT_BUCKET }}
      RELEASE_VERSION: ${{ needs.preflight.outputs.release_version }}
      RELEASE_TAG: ${{ needs.preflight.outputs.release_tag }}
```

Change the deploy checkout to:

```yaml
      - name: Checkout release commit
        uses: actions/checkout@v4
        with:
          ref: ${{ github.sha }}
          fetch-depth: 0
```

Keep all existing test, build, S3 upload, Lambda publish, alias update and alias verification steps in their current order.

- [ ] **Step 3: alias 검증 후 태그와 GitHub Release를 게시한다**

Insert immediately after `Verify alias version`:

```yaml
      - name: Publish Git tag and GitHub Release
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git tag -a "$RELEASE_TAG" "$GITHUB_SHA" -m "$RELEASE_TAG 프로덕션 배포"
          git push origin "$RELEASE_TAG"
          gh release create "$RELEASE_TAG" \
            --verify-tag \
            --target "$GITHUB_SHA" \
            --title "$RELEASE_TAG" \
            --generate-notes
```

Do not add cleanup that deletes or moves a tag when `gh release create` fails.

- [ ] **Step 4: 배포 summary와 metadata를 확장한다**

Add these summary lines before the Lambda version lines:

```yaml
            echo "- Release version: \`${RELEASE_VERSION}\`"
            echo "- Release tag: \`${RELEASE_TAG}\`"
            echo "- Commit SHA: \`${{ steps.metadata.outputs.commit_sha }}\`"
```

Add these metadata keys before `function_name`:

```yaml
            echo "release_version=${RELEASE_VERSION}"
            echo "release_tag=${RELEASE_TAG}"
            echo "commit_sha=${{ steps.metadata.outputs.commit_sha }}"
```

- [ ] **Step 5: YAML 문법과 필수 구조를 검증한다**

Run:

```bash
python3 -c 'import yaml; yaml.safe_load(open(".github/workflows/deploy-prod-lambda.yml")); print("yaml-ok")'
rg -n 'version:|contents: write|prod-lambda-deploy|Validate main branch|release_version|release_tag|ref: \$\{\{ github.sha \}\}|Publish Git tag and GitHub Release|gh release create' .github/workflows/deploy-prod-lambda.yml
```

Expected: `yaml-ok`가 출력되고 모든 필수 구문이 한 번 이상 확인된다.

- [ ] **Step 6: preflight shell 로직의 성공과 실패를 검증한다**

Extract and execute the two preflight `run` blocks from the workflow. Use a temporary Git repository for duplicate-tag verification.

```bash
workflow_file="$(pwd)/.github/workflows/deploy-prod-lambda.yml"

preflight_script() {
  local step_name="$1"
  awk -v name="$step_name" '
    $0 ~ "- name: " name { found = 1 }
    found && /run: \|/ { script = 1; next }
    script && /^$/ { print; next }
    script && /^          / { sub(/^          /, ""); print; next }
    script { exit }
  ' "$workflow_file"
}

GITHUB_REF=refs/heads/main bash <(preflight_script 'Validate main branch')
! GITHUB_REF=refs/heads/dev bash <(preflight_script 'Validate main branch')

release_test_dir=$(mktemp -d)
git -C "$release_test_dir" init
git -C "$release_test_dir" config user.name test
git -C "$release_test_dir" config user.email test@example.com
git -C "$release_test_dir" commit --allow-empty -m init

(cd "$release_test_dir" && RELEASE_VERSION=1.2.3 GITHUB_OUTPUT=/dev/null bash <(preflight_script 'Validate release version'))
! (cd "$release_test_dir" && RELEASE_VERSION=1.2 GITHUB_OUTPUT=/dev/null bash <(preflight_script 'Validate release version'))
git -C "$release_test_dir" tag be-v1.2.3
! (cd "$release_test_dir" && RELEASE_VERSION=1.2.3 GITHUB_OUTPUT=/dev/null bash <(preflight_script 'Validate release version'))
```

Expected: `main`과 새 `1.2.3`만 성공하고 `dev`, `1.2`, 기존 `be-v1.2.3`은 실패한다.

- [ ] **Step 7: 애플리케이션 회귀 테스트와 diff 검사를 실행한다**

Run:

```bash
./gradlew test --stacktrace --no-daemon
git diff --check
```

Expected: `BUILD SUCCESSFUL`이며 `git diff --check` 출력이 없다.

- [ ] **Step 8: workflow 변경을 커밋한다**

```bash
git add .github/workflows/deploy-prod-lambda.yml
git commit -m "306 deploy: Lambda 릴리즈 버전 자동화 적용"
```

---

### Task 3: 전체 변경을 최종 검증한다

**Files:**
- Verify: `AGENTS.md`
- Verify: `CONTRIBUTING.md`
- Verify: `docs/specs/README.md`
- Verify: `docs/tasks/306/design.md`
- Verify: `docs/tasks/306/plan.md`
- Verify: `.github/workflows/deploy-prod-lambda.yml`
- Verify deleted: `scripts/new-spec.sh`

**Interfaces:**
- Consumes: Task 1의 문서 규칙과 Task 2의 workflow 출력.
- Produces: 이슈 #306 완료 조건별 검증 증거와 리뷰 가능한 커밋 집합.

- [ ] **Step 1: 설계 요구사항 누락 여부를 확인한다**

Run:

```bash
rg -n '^## |docs/tasks|release/v|fix/|hotfix/|be-v|GitHub Release|github.sha|Lambda' docs/tasks/306/design.md AGENTS.md CONTRIBUTING.md .github/workflows/deploy-prod-lambda.yml
```

Expected: 설계의 문서 체계, 브랜치, 버전, 동일 SHA, Lambda 추적 요구사항이 모두 대응된다.

- [ ] **Step 2: 전체 검증을 다시 실행한다**

Run:

```bash
python3 -c 'import yaml; yaml.safe_load(open(".github/workflows/deploy-prod-lambda.yml")); print("yaml-ok")'
./gradlew test --stacktrace --no-daemon
git diff --check
git status --short --branch
```

Expected: YAML과 Gradle 테스트가 통과하고 whitespace 오류가 없으며 계획된 파일 외 변경이 없다.

- [ ] **Step 3: 커밋 단위와 최종 diff를 검토한다**

Run:

```bash
git log --oneline origin/dev..HEAD
git diff --stat origin/dev...HEAD
git diff --check origin/dev...HEAD
```

Expected: 설계, 문서 체계, workflow가 의미 있는 커밋으로 분리되고 이슈 #306 범위 밖 변경이 없다.
