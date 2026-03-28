# Tasks – Issue #198 CI Gate

## Checklist
- [x] `.github/workflows/ci.yml` 추가 및 이벤트/Job 구조 정의
- [x] Gradle 캐시 및 Setup Java 스텝 구성
- [x] `./gradlew test --stacktrace` 스텝 추가
- [x] `./gradlew check --stacktrace` 스텝 추가
- [x] 실패 시 Job summary에 안내 메시지 작성
- [x] 로컬 `./gradlew test` 실행 결과 기록
- [x] `deploy-dev-lambda.yml`에 테스트 스텝 삽입
- [x] `deploy-prod-lambda.yml`에 테스트 스텝 삽입

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test` | PASS | 2026-03-28 |
| 2 | `./gradlew test` | PASS | 2026-03-28 |

## Notes
- Observation: Workflow는 PR/Push 모두에서 실행되므로 concurrency group 충돌이 없도록 기본 설정을 사용한다.
