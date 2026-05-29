# Checklist

- [x] origin/dev 기준 `feat/238-dev` worktree 생성
- [x] 현재 학업 기록 분류 로직 확인
- [x] `복핵` 전공 분류 실패 테스트 추가 및 RED 확인
- [x] `복핵`을 전공 분류에 포함
- [x] 관련 테스트 실행
- [x] 전체 테스트 및 빌드 실행

## Verification Log

| Command | Result | Date |
|---------|--------|------|
| `./gradlew test --tests "*AcademicRecordServiceUnitTests"` | Fail as RED: `복핵` not in major list | 2026-05-26 |
| `./gradlew test --tests "*AcademicRecordServiceUnitTests"` | Pass | 2026-05-26 |
| `./gradlew test` | Pass | 2026-05-26 |
| `./gradlew build` | Pass | 2026-05-26 |
| `git diff --check` | Pass | 2026-05-26 |
