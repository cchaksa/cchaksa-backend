# Conflict Resolution Checklist

- [x] 최신 `origin/dev`를 `feat/226` worktree에 병합한다.
- [x] `CourseOfferingService` 충돌을 #208 raw faculty division 구조와 #226 backfill 로직을 모두 보존하도록 해결한다.
- [x] 자동 병합된 `CourseOffering`, `GraduationQueryRepository`, `CourseOfferingServiceUnitTests`가 의미적으로 양쪽 요구사항을 모두 보존하는지 검토한다.
- [x] 충돌 관련 targeted tests를 실행한다.
- [x] 전체 `./gradlew test`를 실행한다.
- [x] merge commit을 생성하고 `origin/feat/226`에 push한다.
