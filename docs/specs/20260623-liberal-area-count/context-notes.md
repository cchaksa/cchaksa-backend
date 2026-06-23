# Context Notes

- 2026-06-23: 사용자 제보 스크린샷에서 선택교양이 `7개영역 / 6개영역`으로 보이나 실제 고유 세부영역은 6개로 확인됨.
- 현재 `GraduationQueryRepository`는 `completedElectiveCourses`를 `offeringId` 고유 개수로 계산한다.
- `liberalAreaCode`는 `course_offerings.area_code`에서 조회되며 선교 과목 응답에만 노출된다.
- 정책 결정: 선교 영역은 `liberalAreaCode` 고유 개수로 카운트한다. null 세부영역은 중복 여부를 판단할 수 없으므로 영역 충족 카운트에서는 제외하고 과목 목록에는 유지한다.
- focused test `./gradlew test --tests com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepositoryEtcTests`는 구현 전 `completedElectiveCourses` 기대값 불일치로 실패했고, 구현 후 통과했다.
- 전체 `./gradlew test`도 통과했다.
- main 대상 PR은 `hotfix/liberal-area-count`, dev 대상 PR은 `hotfix/liberal-area-count-dev`로 분리했다.
