# Issue 230 Tasks

## Checklist
- [x] Portal mapper tests written
- [x] Student graduation progress tests written
- [x] Portal sync tests updated
- [x] Graduation response tests updated
- [x] Implementation completed
- [x] Documentation reviewed
- [x] Static OpenAPI document updated
- [x] Language certification policy model tests written
- [x] Language certification policy entities and enums added
- [x] Policy model tests executed
- [x] Language certification seed SQL added
- [x] Language certification requirement repository/service/controller tests written
- [x] Language certification requirement API implemented
- [x] Static OpenAPI document updated for requirement API

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew compileTestJava` | passed after expected RED failures were implemented | 2026-05-25 |
| 2 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*PortalDataMapperTests" --tests "*StudentGraduationProgressServiceTests" --tests "*PortalSyncServiceTests" --tests "*GraduationServiceTests" --tests "*GraduationControllerApiIntegrationTest"` | passed | 2026-05-25 |
| 3 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test` | passed after removing `checked_at` updates | 2026-05-25 |
| 4 | `ruby -e 'require "yaml"; YAML.load_file("src/main/resources/public/openapi.yaml")'` | passed | 2026-05-25 |
| 5 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*StudentGraduationProgressServiceTests" --tests "*PortalSyncServiceTests"` | passed | 2026-05-25 |
| 6 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCertPolicyModelTests"` | failed as expected: missing policy model classes | 2026-05-26 |
| 7 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCertPolicyModelTests"` | passed | 2026-05-26 |
| 8 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCertPolicyModelTests"` | passed, up-to-date | 2026-05-26 |
| 9 | `git diff --check` | passed | 2026-05-26 |
| 10 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCert*"` | failed as expected: missing requirement DTO, repositories, and service | 2026-05-27 |
| 11 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCert*"` | passed after repository/service/controller implementation | 2026-05-27 |
| 12 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCert*"` | passed after SQL updates | 2026-05-27 |
| 13 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCert*"` | passed after OpenAPI update | 2026-05-27 |
| 14 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*GraduationControllerApiIntegrationTest" --tests "*LanguageCert*"` | passed after sandbox lock retry with approval | 2026-05-27 |
| 15 | `ruby -e 'require "yaml"; YAML.load_file("src/main/resources/public/openapi.yaml")'` | passed | 2026-05-27 |
| 16 | `git diff --check` | passed | 2026-05-27 |
| 17 | `awk ... docs/sql/20260527-language-cert-policy-seed.sql` | passed: policy_groups=17, requirements=151, mappings=384, VERIFIED=331, INFERRED=15, UNMAPPED=38 | 2026-05-27 |
| 18 | `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCert*"` | passed after import cleanup | 2026-05-27 |
| 19 | `git diff --check` | passed after import cleanup | 2026-05-27 |

## Notes
- Observation: 커밋은 사용자가 직접 수행한다.
- Observation: 기본 `java`가 24.0.1이라 `./gradlew test`는 Gradle test task 구성 중 `Type T not present`로 실패한다. Java 17로 Gradle을 실행하면 통과한다.
- Observation: 정적 OpenAPI 문서에 `/api/graduation/progress`와 외국어 인증 응답 필드를 추가했다.
- Observation: 2026-05-26 사용자 승인으로 외국어 인증 기준 정책 엔티티와 enum을 `feat/230`에서 추가한다.
- Observation: 2026-05-27 사용자 승인으로 외국어 인증 기준 seed SQL과 조회 API를 추가한다.
- Observation: CSV 384개 학과 코드 매핑 seed 상태는 `VERIFIED` 331개, `INFERRED` 15개, `UNMAPPED` 38개다.
