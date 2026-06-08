# Context Notes

- 2026-05-26: 사용자는 미지원 이수 구분일 때만 원본 문자열을 DB에 보존하는 방향을 선택했다.
- 2026-05-26: 기존 API에는 영향을 주지 않아야 하므로 `rawFacultyDivisionName`은 엔티티 내부 필드로만 두고 DTO에는 추가하지 않는다.
- 2026-05-26: canonical `facultyDivisionName`은 계속 `FacultyDivision.기타`로 저장해 기존 졸업 진행/학업 기록 API의 `기타` 묶음 동작을 유지한다.
- 2026-05-26: raw 값은 향후 `RT`, `교직` 등을 정식 enum으로 승격할 때 마이그레이션 근거로 사용한다.
- 2026-05-26: `CourseOfferingKey`는 canonical 이수 구분과 raw 이수 구분을 함께 비교해 같은 미지원 값 재연동은 재사용하고 서로 다른 미지원 값은 내부적으로 구분한다.
- 2026-05-26: 1차 작업에서는 학업 기록 DTO와 졸업 진행 DTO에 raw 필드를 추가하지 않았다.
- 2026-05-26: 후속 요청으로 학업 기록 과목 DTO에는 `rawAreaType`을 추가한다. `areaType`은 계속 `기타`로 유지해 기존 분류 계약을 깨지 않는다.
