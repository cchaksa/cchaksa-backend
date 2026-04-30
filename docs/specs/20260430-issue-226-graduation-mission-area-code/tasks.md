# Tasks

## Checklist
- [x] Domain tests written — N/A (도메인 변경 없음)
- [x] Application layer updated — N/A (graduation 모듈은 application layer 분리 없음)
- [x] Infrastructure layer updated — `GraduationQueryRepository.getLatestValidCourses` SQL에 `co.area_code` 추가, row 매핑에 신규 컬럼 반영, `toCourseResponseDto` 매퍼에서 `liberalAreaCode` 패스스루 (단일전공·복수전공 흐름에 동시 적용)
- [x] Internal DTO updated — `CourseInternalDto`에 `liberalAreaCode: Integer (nullable)` 필드 추가
- [x] Global/config reviewed — `application.yml`의 `spring.jackson`에는 `default-property-inclusion` 미설정. 기존 응답 DTO 컨벤션(`ErrorResponse`/`SuccessResponse`/`ErrorDetail`)을 따라 `CourseDto` 클래스 레벨에 `@JsonInclude(JsonInclude.Include.NON_NULL)` 적용 (clarify Decision 5)
- [x] API/controller updated — `CourseDto`에 `liberalAreaCode` 필드 + `@Schema(description, example, nullable = true)` + 클래스 레벨 `@JsonInclude(NON_NULL)`
- [x] Documentation updated — 본 spec 번들. `src/main/resources/public/openapi.yaml`에는 graduation 스키마 정의가 없어 변경 불요(springdoc이 `@Schema` 어노테이션으로 자동 생성)
- [x] Tests added — `CourseDtoJsonTest` (직렬화 3건: 정수 포함 / null omit / 기존 필드 보존) + `GraduationQueryRepositoryMapperTest` (매퍼 패스스루 2건: 정수 / null)

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test` | PASS — 신규 테스트 5건 PASS, 전체 suite failures=0 | 2026-04-30 |

## Notes
- Observation: `original_area_code`(이전 분석상 dead column, 졸업 로직에서 미사용)와 본 스펙에서 사용하는 `area_code`(LiberalArtsAreaCode FK)는 다른 컬럼이다. 혼동 주의.
- Observation: graduation 모듈은 application layer 분리가 없어 Phase 3는 무영향 — Phase 4(repository + 매퍼 + Internal DTO) + Phase 6(CourseDto) + Phase 5(Jackson 설정 점검)가 핵심.
- Observation: `toCourseResponseDto` 매퍼는 단일전공·복수전공에서 공유되며, 본 스펙은 양쪽 모두 적용한다(clarify Decision 4). 매퍼 분리 없음.
- Observation: `CourseInternalDto`의 `@AllArgsConstructor` 시그니처 변경에 따라 기존 `GraduationServiceTests`의 `CourseDto` 생성 호출 1곳(line 239)을 6번째 인자 `null`로 보정.
- Observation: SQL `DISTINCT ON (c.course_code, co.faculty_division_name)` 절은 그대로 유지. `co.area_code`는 distinct 결정 컬럼이 아니므로 동일 row가 선택됨.
