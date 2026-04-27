# Clarify – Issue #198 CI Gate

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 정적 분석 범위를 `./gradlew check`로 대체해도 되는가? | Codex | Closed (2026-03-28 사용자 “정적 분석” 요청을 `check`로 수렴) |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 새 workflow는 `.github/workflows/ci.yml` 단일 파일로 관리한다. | 기존 workflow와 충돌을 피하고 Branch Protection 대상 Job 이름을 명확히 하기 위함 | 2026-03-28 |
| 2 | Static analysis 명령은 `./gradlew check --stacktrace`로 실행한다. | 현재 프로젝트에 별도 정적 분석 플러그인이 없으므로 `check`를 훅 포인트로 사용하고 향후 도구 추가 시 자동 실행 보장 | 2026-03-28 |

## Risks / Unknowns
- Item: `gradlew check`가 테스트와 중복되어 실행 시간이 길어질 수 있음
  - Impact: CI 대기 시간이 증가해 피드백이 늦어질 수 있다.
  - Mitigation: Gradle 캐시를 적극 사용하고, 이후 실제 정적 분석 도구 도입 시 병렬 Job 분리를 검토한다.

## Follow-ups
- [ ] Branch Protection에 새 Job(`ci-gradle`)을 필수 검사로 등록 (Owner: Repo Admin)
