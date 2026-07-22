# CodeRabbit PR Review Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strengthen the existing CodeRabbit configuration for the Haksa backend, verify it automatically, and publish a PR that can confirm the GitHub App transition before Gemini Code Assist access is removed.

**Architecture:** Keep `.coderabbit.yaml` as the single repository policy file and add one focused JUnit configuration test using the existing SnakeYAML dependency. Treat repository configuration, CodeRabbit App authorization, and Gemini App removal as separate gates so a failed external setup cannot be mistaken for a YAML defect.

**Tech Stack:** CodeRabbit schema v2, YAML, Java 17, JUnit 5, AssertJ, SnakeYAML, GitHub Apps.

## Global Constraints

- Preserve `language: ko-KR` and `reviews.profile: chill`.
- Review only actionable defects, security risks, contract violations, and regressions.
- Skip Draft PRs and enable incremental reviews after later pushes.
- The default branch `dev` is reviewed automatically; `base_branches` lists only `main` and `release/*` as additional targets.
- Do not add linked repositories, change application behavior, or modify deployment workflows.
- Do not remove Gemini Code Assist access before CodeRabbit is verified on a Ready PR.

---

### Task 1: Lock the CodeRabbit policy with a configuration test

**Files:**
- Create: `src/test/java/com/chukchuk/haksa/global/config/CodeRabbitConfigTest.java`

**Interfaces:**
- Consumes: repository-root `.coderabbit.yaml` and `org.yaml.snakeyaml.Yaml` already available in the test runtime.
- Produces: parsed `Map<String, Object>` assertions for common review policy, automatic review targets, and required path instructions.

- [x] **Step 1: Add a failing structured YAML test.**

Create `CodeRabbitConfigTest` with a Korean file header. Load `.coderabbit.yaml` through `new Yaml().load(inputStream)` and assert these exact values.

```java
assertThat(config.get("language")).isEqualTo("ko-KR");
assertThat(reviews.get("profile")).isEqualTo("chill");
assertThat(reviews.get("poem")).isEqualTo(false);
assertThat(reviews.get("in_progress_fortune")).isEqualTo(false);
assertThat(autoReview.get("enabled")).isEqualTo(true);
assertThat(autoReview.get("auto_incremental_review")).isEqualTo(true);
assertThat(autoReview.get("drafts")).isEqualTo(false);
assertThat(autoReview.get("base_branches"))
        .isEqualTo(List.of("^main$", "^release/.*$"));
```

Parse `reviews.path_instructions` and assert that it contains these exact path globs.

```java
assertThat(paths).containsExactly(
        "src/main/java/com/chukchuk/haksa/global/security/**",
        "src/main/java/com/chukchuk/haksa/domain/auth/**",
        "src/main/java/com/chukchuk/haksa/domain/**",
        "src/main/java/com/chukchuk/haksa/application/**",
        "src/main/java/com/chukchuk/haksa/application/portal/**",
        "src/main/java/com/chukchuk/haksa/infrastructure/portal/**",
        "src/main/resources/db/migration/**",
        "src/main/java/com/chukchuk/haksa/global/logging/**",
        "src/main/java/com/chukchuk/haksa/global/exception/**",
        "src/test/**"
);
```

- [x] **Step 2: Run the focused test and confirm the current minimal configuration fails.**

Run: `./gradlew test --tests com.chukchuk.haksa.global.config.CodeRabbitConfigTest --stacktrace --no-daemon`

Expected: FAIL because `poem` is true and incremental review, extra base branches, fortune, and path instructions are absent.

- [x] **Step 3: Keep the failing test uncommitted and proceed to Task 2.**

Record the expected assertion failure in the plan verification notes. Do not create a commit while the focused test is failing.

### Task 2: Apply the Haksa backend review policy

**Files:**
- Modify: `.coderabbit.yaml`
- Test: `src/test/java/com/chukchuk/haksa/global/config/CodeRabbitConfigTest.java`

**Interfaces:**
- Consumes: CodeRabbit schema v2 properties `tone_instructions`, `reviews.auto_review`, and `reviews.path_instructions`.
- Produces: a repository policy that CodeRabbit reads from the PR base branch.

- [x] **Step 1: Add the official schema header and common review policy.**

Add `# yaml-language-server: $schema=https://coderabbit.ai/integrations/schema.v2.json`, set `poem: false`, `in_progress_fortune: false`, and add concise Korean `tone_instructions` that excludes style-only, CI-format, and out-of-scope refactoring comments.

- [x] **Step 2: Configure automatic review behavior.**

Set `auto_incremental_review: true`, preserve `drafts: false`, and add the exact additional base patterns below.

```yaml
base_branches:
  - "^main$"
  - "^release/.*$"
```

- [x] **Step 3: Add repository-specific path instructions.**

Add the ten path globs from Task 1 in that order. Their instructions must cover JWT and authorization bypass, transaction and concurrency defects, portal callback validation and idempotency, forward-only Flyway compatibility, Sentry and log PII, and regression-focused tests.

- [x] **Step 4: Run the focused test and verify it passes.**

Run: `./gradlew test --tests com.chukchuk.haksa.global.config.CodeRabbitConfigTest --stacktrace --no-daemon`

Expected: PASS.

- [x] **Step 5: Commit the reviewed configuration.**

```bash
git add .coderabbit.yaml src/test/java/com/chukchuk/haksa/global/config/CodeRabbitConfigTest.java
git commit -m "319 chore: CodeRabbit 백엔드 리뷰 정책 보강"
```

### Task 3: Verify and publish the transition

**Files:**
- Modify: `docs/tasks/319/plan.md` only to record completed checks or implementation discoveries.
- Update externally: GitHub issue #319 and a `feat/319` to `dev` pull request.

**Interfaces:**
- Consumes: committed `.coderabbit.yaml`, passing Gradle tests, GitHub App access, and PR review events.
- Produces: a clean pushed branch, an issue body aligned with the pre-existing config, and a Ready PR suitable for CodeRabbit verification.

- [x] **Step 1: Run the complete repository verification.**

Run: `./gradlew test --stacktrace --no-daemon`

Expected: BUILD SUCCESSFUL with zero failed tests.

- [x] **Step 2: Inspect repository scope and policy safety.**

Run: `git diff --check origin/dev...HEAD`, `git status --short`, and `git diff --stat origin/dev...HEAD`.

Expected: only `.coderabbit.yaml`, the focused test, and `docs/tasks/319/*` are changed; no application, API, DB, or workflow files are modified.

- [x] **Step 3: Update issue #319.**

Replace the inaccurate “add `.coderabbit.yaml`” wording with “strengthen the existing `.coderabbit.yaml`”. Preserve the external gates for CodeRabbit App access, Ready/incremental review verification, and Gemini App removal after successful verification.

**Verification notes:**

- RED: the focused test failed against the original minimal configuration because the required review policy and path instructions were absent.
- GREEN: `./gradlew test --tests com.chukchuk.haksa.global.config.CodeRabbitConfigTest --stacktrace --no-daemon` passed after the configuration update.
- Regression: `./gradlew test --stacktrace --no-daemon` passed with `BUILD SUCCESSFUL`.
- Scope: `git diff --check origin/dev...HEAD` passed, and the branch changes are limited to `.coderabbit.yaml`, the focused test, and `docs/tasks/319/*`.
- Issue: #319 now describes strengthening the existing configuration and keeps CodeRabbit App verification and Gemini removal as separate external gates.
- PR: Ready PR #320 targets `dev`, and its remote head matched the local branch at creation time.
- App access: the existing CodeRabbit installation initially allowed only `cchaksa-app`; `cchaksa-backend` was added as the second selected repository and the saved setting was rechecked.
- Review: CodeRabbit acknowledged `@coderabbitai review`, reviewed all four changed files, posted two findings, and completed with a successful status check.
- Configuration activation: CodeRabbit reported that open source PRs use configuration from the base branch, so this PR verifies App and incremental behavior while the strengthened policy becomes active after merge.
- Incremental review: later pushes each created a CodeRabbit status on the new HEAD, and the latest review-fix commit completed successfully.
- Gemini gate: `cchaksa-backend` was removed from Gemini Code Assist repository access after CodeRabbit verification. Terraform and scraper access remains unchanged.

- [x] **Step 4: Record verification and commit the plan.**

```bash
git add docs/tasks/319/design.md docs/tasks/319/plan.md
git commit -m "319 docs: CodeRabbit 전환 검증 결과 반영"
```

- [x] **Step 5: Push and create a Ready PR to `dev`.**

Push `feat/319`, create a Korean PR using the repository's current PR structure, and link `Closes #319`. Verify the remote head SHA equals local HEAD and the PR base is `dev`.

- [x] **Step 6: Check the external review gate.**

Confirm whether CodeRabbit creates a review or a skip/configuration message. If no CodeRabbit event appears, report CodeRabbit App installation as the remaining blocker and do not claim Gemini removal is complete. Remove Gemini access only after CodeRabbit review is proven.
