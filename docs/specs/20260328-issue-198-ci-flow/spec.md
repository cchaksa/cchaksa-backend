# Spec – Issue #198 CI Gate

## 1. Feature Overview
- Purpose: Pull Request/Push 이벤트마다 `./gradlew test`와 정적 분석(`./gradlew check`)을 자동 실행하는 CI 워크플로우를 추가해 실패 시 병합을 차단한다.
- Scope
  - In: GitHub Actions workflow 추가/구성(`.github/workflows/ci.yml`), Gradle 캐시 사용, pull_request/push 대상 브랜치 필터 정의, 실패 시 Job status로 전달.
  - In-Δ (2026-03-28): `deploy-*lambda.yml`에 Gradle 테스트 단계를 삽입해 배포 전 필수 검증을 수행한다.
  - Out: 애플리케이션 코드/의존성 변경, 배포 workflow의 Lambda zip 로직 외 기타 배포 전략.
- Expected Impact: 자동 테스트 없이 merge되던 리스크를 제거하고, 브랜치 보호 규칙에서 새 Job을 필수로 설정할 수 있게 한다.
- Stakeholder Confirmation: 2026-03-28 사용자 지시(“PR/푸시용 CI workflow를 신설…” – Issue #198).

## 2. Domain Rules
- Rule 1: Workflow는 `pull_request`(base: dev, main)와 `push`(branches: dev, main) 이벤트를 모두 감시한다.
- Rule 2: Job은 `actions/setup-java@v4`로 JDK17을 설치하고 `./gradlew test` → `./gradlew check` 순으로 실행하며 하나라도 실패하면 전체 Job을 실패로 마크한다.
- Rule 3: Gradle 디펜던시 캐시는 `actions/cache@v4` + Gradle 전용 캐시 액션으로 구성해 동일 러너에서 반복 사용한다.
- Rule 4: 모든 Lambda 배포 workflow는 패키징 전에 `./gradlew test --stacktrace --no-daemon`을 실행한다.
- Mutable Rules: 대상 브랜치 목록 (추후 release 브랜치 추가 가능).
- Immutable Rules: 테스트/정적 분석 실패 시 배포/머지를 허용하지 않는다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: PR Validation Pipeline
  - Trigger: 개발자가 dev 기반 PR을 생성하거나 업데이트한다.
  - Actor: GitHub Actions CI Job
  - Steps:
    1. pull_request 이벤트로 워크플로우가 시작된다.
    2. 코드 체크아웃 후 Gradle 캐시를 복원한다.
    3. `./gradlew test --stacktrace`를 실행해 단위/통합 테스트를 수행한다.
    4. `./gradlew check --stacktrace`를 실행해 정적 분석/추가 검증을 수행한다.
    5. 두 단계 모두 성공하면 상태 체크가 green으로 반환된다.
  - Expected Result: PR이 merge 가능 상태가 된다.

### Exception / Boundary Flow
- Scenario Name: Verification Failure
  - Condition: 테스트 또는 `check`가 실패하거나 빌드 환경 설정 오류가 발생한다.
  - Expected Behavior: Job이 실패 상태로 종료되고 GitHub status가 빨간불이 되어 브랜치 보호 규칙이 merge를 차단한다. 실패 로그는 PR Checks 탭에 기록된다.

- Scenario Name: Lambda Deployment Guard
  - Condition: 운영자가 `deploy-dev-lambda.yml` 혹은 `deploy-prod-lambda.yml`을 수동 실행한다.
  - Expected Behavior: Workflow가 패키징 전에 `./gradlew test --stacktrace --no-daemon`을 실행해 실패 시 즉시 배포를 중단한다. 성공하면 기존 `lambdaZip` 빌드 및 배포 단계를 이어간다.

## 4. Transaction / Consistency
- Transaction Start Point: GitHub Actions가 pull_request 혹은 push 이벤트를 수신해 Job을 큐잉한 시점.
- Transaction End Point: CI Job이 성공/실패 상태를 리포트하고 아티팩트를 업로드한 시점.
- Atomicity Scope: 단일 Job(`ci-gradle`) 전체가 하나의 원자 단위이며, 중간 단계 실패는 전체 실패로 간주한다.
- Eventual Consistency Allowed: 허용하지 않음. Green 상태 없이 merge 불가하도록 한다.

## 5. API List (필요 시)
- Endpoint: N/A (GitHub Actions workflow 내부 실행으로 외부 API 노출 없음)

## 6. Exception Policy
- Error Code: `CI-GRADLE-FAIL`
  - Condition: Gradle test 혹은 check 단계가 non-zero 종료 코드를 반환.
  - Message Convention: GitHub Job summary에 실패 단계/명령어를 요약하고 필요한 경우 다음 단계 안내를 작성.
  - Handling Layer: GitHub Actions Job (Logs + Step Summary).
  - User Exposure: PR Checks 탭 및 리포에 이메일/Slack 알림 (GitHub 기본 알림).

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Domain complete
- [ ] Phase 3 Application complete
- [ ] Phase 4 Infrastructure complete
- [ ] Phase 5 Global/Config complete
- [ ] Phase 6 API/Controller complete

## 8. Generated File List
- Path: `.github/workflows/ci.yml`
  - Description: PR/Push 이벤트용 Gradle CI workflow (tests + check)
  - Layer: CI/CD
