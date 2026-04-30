# 졸업 진행도 응답에 과목별 LiberalArtsAreaCode 노출 스펙 (Issue #226)

## 1. Feature Overview
- Purpose: `GET /api/graduation/progress` 응답의 `AreaProgressDto.courses[i]`(`CourseDto`)에 LiberalArtsAreaCode (`course_offerings.area_code`) 값을 추가 노출하여, 프런트가 과목 단위로 세부 영역(선교 등)을 식별할 수 있도록 한다. 선교 영역의 경우 과목 하나하나가 자체 sub-area 코드를 가지므로 그 값을 과목 row마다 그대로 노출한다.
- Scope
  - In: `CourseDto`의 신규 nullable 필드 `liberalAreaCode` 추가. `CourseInternalDto`에 동일 필드 추가. `GraduationQueryRepository.getLatestValidCourses` SQL에서 `co.area_code` 추가 SELECT. `GraduationQueryRepository.toCourseResponseDto` 매퍼에서 `liberalAreaCode` 패스스루. 단일전공(`getStudentAreaProgress`)·복수전공(`getDualMajorAreaProgress`) 두 흐름 모두에 동일 적용 (매퍼 공유 구조 그대로 활용).
  - Out: `AreaProgressDto` 자체 필드 추가 없음. `original_area_code` 컬럼 정정/historical NULL 데이터 backfill, 기존 영역(전필/전선/교필/교선/일선 등)의 응답 형태 변경, 졸업 요건 매칭 알고리즘(`faculty_division_name` 기반)은 변경하지 않음.
- Expected Impact: 프런트가 과목 단위로 sub-area 코드를 받을 수 있게 됨. 추가 nullable 필드만 노출하므로 기존 클라이언트는 무시해도 무방한 backward-compatible 변경.
- Stakeholder Confirmation: PO 구두 요청 (Issue #226, 2026-04-30 컨펌; 2026-04-30 기능 명세 정정 — 위치를 AreaProgressDto에서 CourseDto로 변경; 2026-04-30 추가 결정 — 단일전공·복수전공 양쪽 적용).

## 2. Domain Rules
- Rule 1: `liberalAreaCode`는 `CourseDto`에 위치하는 nullable 필드이다. JSON 응답에서는 `@JsonInclude(NON_NULL)` 정책에 의해 null이면 키 자체가 omit된다.
- Rule 2: `liberalAreaCode`의 값은 해당 과목의 `course_offerings.area_code`를 그대로 패스스루한다. 영역 단위 집계나 변환을 하지 않는다.
- Rule 3: 선교(faculty_division_name = '선교') 과목은 자체 area_code를 보유하므로 `CourseDto.liberalAreaCode`에 정수 값으로 노출된다. 비-선교 영역 과목 또는 영역 sub-category가 없는 row는 자연스럽게 NULL을 반환하여 omit된다.
- Rule 4: historical NULL 데이터로 인해 일부 선교 row의 `area_code`가 NULL인 경우, 그 과목의 `liberalAreaCode`만 null이 되어 omit된다. 같은 영역 안의 다른 과목 row에는 영향이 없다.
- Rule 5: `liberalAreaCode`는 표시 전용 필드로, 졸업 요건 매칭 알고리즘(`faculty_division_name` 기반 그룹화)에는 영향을 주지 않는다.
- Mutable Rules: 향후 다른 영역에도 sub-area 식별이 필요하면 동일 필드를 재사용한다(필드 의미는 "course_offerings.area_code" 그대로이므로 확장 가능).
- Immutable Rules: `CourseDto`의 기존 필드(`year`, `courseName`, `credits`, `grade`, `semester`)와 `AreaProgressDto`의 모든 기존 필드 의미는 그대로 유지한다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 단일전공 학생이 선교 과목 N건을 이수한 경우의 졸업 진행도 조회
  - Trigger: 인증된 단일전공 학생이 `GET /api/graduation/progress` 호출
  - Actor: GraduationController → GraduationService → GraduationQueryRepository (`getStudentAreaProgress`)
  - Steps:
    1. `getLatestValidCourses(studentId)`가 `co.area_code` 컬럼을 포함해 수강 이력을 조회한다.
    2. 매핑 단계에서 `CourseInternalDto`에 `liberalAreaCode`가 각 row별로 채워진다.
    3. `getStudentAreaProgress`가 `faculty_division_name`별로 그룹화하여 `AreaProgressDto`를 만든다.
    4. `toCourseResponseDto`가 각 `CourseInternalDto`를 `CourseDto`로 변환할 때 `liberalAreaCode`를 패스스루한다.
  - Expected Result: 응답의 선교 `AreaProgressDto.courses[]` 안의 각 `CourseDto`에 `liberalAreaCode` 정수 값이 포함된다. 비-선교 영역의 `CourseDto`는 `liberalAreaCode`가 null이므로 NON_NULL omit된다.

- Scenario Name: 복수전공 학생이 선교 과목 N건을 이수한 경우의 졸업 진행도 조회
  - Trigger: 인증된 복수전공 학생이 `GET /api/graduation/progress` 호출
  - Actor: GraduationController → GraduationService → GraduationQueryRepository (`getDualMajorAreaProgress`)
  - Steps: 위 단일전공 시나리오와 동일하되 `getDualMajorAreaProgress`가 사용된다. `toCourseResponseDto` 매퍼는 동일 인스턴스가 호출되므로 `liberalAreaCode` 패스스루 동작이 그대로 적용된다.
  - Expected Result: 단일전공과 동일하게 선교 `AreaProgressDto.courses[]`의 각 `CourseDto`에 `liberalAreaCode`가 포함된다. 복수전공 졸업요건에 선교 영역이 등장하지 않는 학과 구조라면 응답에 영향이 없다.

- Scenario Name: 비-선교 영역만 이수한 학생 조회
  - Trigger: 동일 엔드포인트 호출
  - Actor: 동일
  - Steps: 모든 row의 `area_code`가 NULL → 모든 `CourseDto.liberalAreaCode = null` → JSON에서 키 자체가 등장하지 않음.
  - Expected Result: 응답 어디에도 `liberalAreaCode` 키가 노출되지 않는다.

### Exception / Boundary Flow
- Scenario Name: 선교 영역의 일부 row만 historical NULL
  - Condition: 학생이 선교 과목 N건을 이수했지만 그 중 일부의 `area_code`가 NULL.
  - Expected Behavior: 비-NULL 과목은 정수가 노출되고, NULL 과목은 키가 omit된다. 같은 영역 안에서 과목별로 분리 처리된다.

- Scenario Name: 선교 영역의 모든 row가 historical NULL
  - Condition: 학생이 이수한 선교 과목 모두의 `area_code`가 NULL.
  - Expected Behavior: 모든 `CourseDto`에서 `liberalAreaCode` 키가 omit된다. 응답은 정상 처리된다.

- Scenario Name: 학생이 선교 과목을 한 건도 이수하지 않음
  - Condition: 졸업요건에 선교가 있고 `AreaProgressDto`는 생성되지만 `courses` 리스트가 비어있음.
  - Expected Behavior: courses가 빈 리스트로 반환된다. `liberalAreaCode` 관련 영향 없음.

## 4. Transaction / Consistency
- Transaction Start Point: `GraduationController.getGraduationProgress` 진입 (read-only).
- Transaction End Point: 컨트롤러 메서드 종료.
- Atomicity Scope: 단일 read-only 조회. 데이터 변경 없음.
- Eventual Consistency Allowed: N/A (조회 전용).

## 5. API List
- Endpoint: `GET /api/graduation/progress`
  - Method: GET
  - Request DTO: 변경 없음 (`@AuthenticationPrincipal CustomUserDetails` 사용)
  - Response DTO: `GraduationProgressResponse` → `AreaProgressDto.courses[i]` (`CourseDto`)에 `liberalAreaCode: Integer (nullable)` 필드 추가
  - Authorization: 변경 없음
  - Idempotency: 자명 (read-only)
  - 응답 예시 (선교 영역의 courses에 liberalAreaCode 노출):
    ```json
    {
      "areaType": "선교",
      "requiredCredits": 3,
      "earnedCredits": 6,
      "requiredElectiveCourses": null,
      "completedElectiveCourses": 0,
      "totalElectiveCourses": null,
      "courses": [
        {
          "year": 2024,
          "courseName": "기독교의 이해",
          "credits": 3,
          "grade": "A+",
          "semester": 10,
          "liberalAreaCode": 7
        },
        {
          "year": 2024,
          "courseName": "현대인과 성서",
          "credits": 3,
          "grade": "A0",
          "semester": 20,
          "liberalAreaCode": 8
        }
      ]
    }
    ```
  - 응답 예시 (비-선교 영역의 courses는 liberalAreaCode omit):
    ```json
    {
      "areaType": "전공필수",
      "requiredCredits": 24,
      "earnedCredits": 21,
      "requiredElectiveCourses": null,
      "completedElectiveCourses": 0,
      "totalElectiveCourses": null,
      "courses": [
        {
          "year": 2023,
          "courseName": "자료구조",
          "credits": 3,
          "grade": "A+",
          "semester": 20
        }
      ]
    }
    ```

## 6. Exception Policy
- 신규 ErrorCode 없음. 기존 `GRADUATION_REQUIREMENTS_DATA_NOT_FOUND` 등 정책 그대로 유지.
- `liberalAreaCode` 매핑은 단순 패스스루이며 별도 예외 정책 불요. SQL 예외 등은 기존 흐름에 위임.

## 7. Phase Checklist
- [x] Phase 1 Spec fixed (이 문서 + clarify/plan/tasks 승인)
- [x] Phase 2 Domain complete — 영향 없음 (CourseOffering/LiberalArtsAreaCode/FacultyDivision 변경 없음)
- [x] Phase 3 Application complete — 영향 없음 (graduation 모듈은 application layer 분리가 없는 구조)
- [x] Phase 4 Infrastructure complete — `GraduationQueryRepository.getLatestValidCourses` SQL 컬럼 추가 + 매핑 변경, `toCourseResponseDto` 매퍼 변경 (단일전공·복수전공 흐름에 동시 반영), `CourseInternalDto` 필드 추가
- [x] Phase 5 Global/Config complete — 글로벌 Jackson `default-property-inclusion` 미설정 확인 → `CourseDto` 클래스 레벨 `@JsonInclude(NON_NULL)` 적용 (clarify Decision 5)
- [x] Phase 6 API/Controller complete — `CourseDto`에 `liberalAreaCode` 필드 + `@Schema(nullable = true)` + 클래스 레벨 `@JsonInclude(NON_NULL)` 적용. 컨트롤러 변경 없음. `openapi.yaml`은 graduation 스키마 미정의로 변경 불요

## 8. Generated File List
- `docs/specs/20260430-issue-226-graduation-mission-area-code/`
  - Description: Issue #226 스펙 번들 (spec/clarify/plan/tasks)
  - Layer: Docs
- `src/main/java/com/chukchuk/haksa/domain/graduation/dto/CourseDto.java`
  - Description: `liberalAreaCode: Integer (nullable)` 필드 + `@Schema` + (필요 시) `@JsonInclude(NON_NULL)` 추가
  - Layer: API DTO
- `src/main/java/com/chukchuk/haksa/domain/graduation/dto/CourseInternalDto.java`
  - Description: SQL→매핑용 `liberalAreaCode: Integer (nullable)` 필드 추가
  - Layer: Internal DTO
- `src/main/java/com/chukchuk/haksa/domain/graduation/repository/GraduationQueryRepository.java`
  - Description: `getLatestValidCourses` SQL에 `co.area_code` 추가, row→`CourseInternalDto` 매핑 보강. `toCourseResponseDto`에서 `liberalAreaCode` 패스스루. 매퍼 공유 구조로 `getStudentAreaProgress` 및 `getDualMajorAreaProgress` 두 흐름에 자동 반영됨.
  - Layer: Repository (Native Query) + 내부 매퍼
- `src/test/java/com/chukchuk/haksa/domain/graduation/dto/CourseDtoJsonTest.java`
  - Description: NON_NULL 직렬화 검증 — liberalAreaCode가 null이면 키 omit, 정수면 포함
  - Layer: Test
- `src/test/java/com/chukchuk/haksa/domain/graduation/repository/GraduationQueryRepositoryTest.java` (신설 또는 기존 확장)
  - Description: 선교 과목 row의 area_code 패스스루, historical NULL 케이스, 비-선교 영역의 NULL omit 검증
  - Layer: Test
