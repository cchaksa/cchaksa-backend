# Context Notes

## 2026-05-26 PR #231 conflict resolution

- 현재 PR head는 `origin/feat/226`, base는 `origin/dev`다.
- `origin/dev`는 merge-base `fd07c6c` 이후 #208 raw faculty division / 기타 이수구분 처리를 포함한다.
- `origin/feat/226`은 선교 `area_code` 응답 노출과 historical NULL backfill을 포함한다.
- 실제 content conflict는 `CourseOfferingService.java`에서 발생한다.
- 해결 원칙은 #208의 `FacultyDivisionResolution`, `rawFacultyDivisionName`, 확장된 `CourseOfferingKey` 구조를 유지하고, #226의 기존 row reuse backfill 호출 및 helper를 그 구조 위에 보존하는 것이다.
- `GraduationQueryRepositoryEtcTests`는 #208에서 추가된 테스트였고 #226의 `CourseInternalDto.liberalAreaCode` 생성자 인자를 모르고 있어 컴파일에 실패했다. 각 `CourseInternalDto` 생성 호출에 `null`을 명시해 현재 DTO 시그니처와 맞췄다.
- Targeted verification: `./gradlew test --tests com.chukchuk.haksa.domain.course.service.CourseOfferingServiceUnitTests --tests com.chukchuk.haksa.domain.course.model.CourseOfferingBackfillTest --tests com.chukchuk.haksa.domain.graduation.dto.CourseDtoJsonTest --tests com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepositoryMapperTest --tests com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepositoryEtcTests --tests com.chukchuk.haksa.domain.academic.record.dto.StudentCourseDtoTests --tests com.chukchuk.haksa.domain.academic.record.service.AcademicRecordServiceUnitTests` passed.
- Full verification: `./gradlew test` passed.
