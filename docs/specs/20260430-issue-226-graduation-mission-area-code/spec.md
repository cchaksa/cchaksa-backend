# 졸업 진행도 응답에 과목별 LiberalArtsAreaCode 노출 스펙 (Issue #226)

## 1. Feature Overview
- Purpose: `GET /api/graduation/progress` 응답의 `AreaProgressDto.courses[i]`(`CourseDto`)에 LiberalArtsAreaCode (`course_offerings.area_code`) 값을 추가 노출하여, 프런트가 과목 단위로 세부 영역(선교 등)을 식별할 수 있도록 한다. 선교 영역의 경우 과목 하나하나가 자체 sub-area 코드를 가지므로 그 값을 과목 row마다 그대로 노출한다.
- Scope
  - In:
    1. **응답 노출 (1차 작업)** — `CourseDto`의 신규 nullable 필드 `liberalAreaCode` 추가. `CourseInternalDto`에 동일 필드 추가. `GraduationQueryRepository.getLatestValidCourses` SQL에서 `co.area_code` 추가 SELECT. `GraduationQueryRepository.toCourseResponseDto` 매퍼에서 `liberalAreaCode` 패스스루. 단일전공(`getStudentAreaProgress`)·복수전공(`getDualMajorAreaProgress`) 두 흐름 모두에 동일 적용 (매퍼 공유 구조 그대로 활용).
    2. **선교 영역 area_code 정합성 보강 (2차 작업, 2026-05-20 추가)** — 재스크래핑(첫 연결/재연결 양쪽) 시 `faculty_division_name = 선교` + `liberalArtsAreaCode IS NULL` + 스크래퍼가 비-NULL non-zero 값을 보낸 경우에만 `course_offerings.area_code`를 단방향 backfill. CourseOffering에 backfill 전용 도메인 메서드 `backfillMissionLiberalAreaCode` 추가(내부 가드 3종으로 다른 용도 차단). `CourseOfferingService.getOrCreateAll`의 기존 row reuse 분기에 backfill 호출 삽입.
  - Out: `AreaProgressDto` 자체 필드 추가 없음. `original_area_code` 컬럼 정정, 비-선교 영역(전필/전선/교필/교선/일선 등) backfill, 졸업 요건 매칭 알고리즘(`faculty_division_name` 기반)은 변경하지 않음. 운영용 일회성 SQL backfill은 본 스펙 범위 외(다른 record 값 복사 시 잘못된 매핑 위험 — PO 결정으로 제외).
- Expected Impact: 프런트가 과목 단위로 sub-area 코드를 받을 수 있게 됨. 추가 nullable 필드만 노출하므로 기존 클라이언트는 무시해도 무방한 backward-compatible 변경.
- Stakeholder Confirmation: PO 구두 요청 (Issue #226, 2026-04-30 컨펌; 2026-04-30 기능 명세 정정 — 위치를 AreaProgressDto에서 CourseDto로 변경; 2026-04-30 추가 결정 — 단일전공·복수전공 양쪽 적용; 2026-05-20 후속 결정 — 선교 영역 area_code historical NULL 정합성 보강 작업을 같은 #226에 묶고 CourseOffering immutable 원칙을 backfill 전용 메서드로 한정 침범).

## 2. Domain Rules
- Rule 1: `liberalAreaCode`는 `CourseDto`에 위치하는 nullable 필드이다. JSON 응답에서는 `@JsonInclude(NON_NULL)` 정책에 의해 null이면 키 자체가 omit된다.
- Rule 2: `liberalAreaCode`의 값은 해당 과목의 `course_offerings.area_code`를 그대로 패스스루한다. 영역 단위 집계나 변환을 하지 않는다.
- Rule 3: 선교(faculty_division_name = '선교') 과목은 자체 area_code를 보유하므로 `CourseDto.liberalAreaCode`에 정수 값으로 노출된다. 비-선교 영역 과목 또는 영역 sub-category가 없는 row는 자연스럽게 NULL을 반환하여 omit된다.
- Rule 4: historical NULL 데이터로 인해 일부 선교 row의 `area_code`가 NULL인 경우, 그 과목의 `liberalAreaCode`만 null이 되어 omit된다. 같은 영역 안의 다른 과목 row에는 영향이 없다.
- Rule 5: `liberalAreaCode`는 표시 전용 필드로, 졸업 요건 매칭 알고리즘(`faculty_division_name` 기반 그룹화)에는 영향을 주지 않는다.
- Rule 6 (Backfill 정합성): 재스크래핑 시 기존 CourseOffering row의 `area_code`는 다음 4개 조건이 모두 충족될 때만 단방향(NULL → non-null)으로 갱신된다. (i) `existing.facultyDivisionName == FacultyDivision.선교`, (ii) `existing.liberalArtsAreaCode == null`, (iii) `cmd.areaCode() != null`, (iv) `cmd.areaCode() != 0`. 한 조건이라도 어긋나면 갱신 없음. 이미 값이 있는 row는 덮어쓰지 않는다.
- Rule 7 (Immutable 보호): CourseOffering의 setter/update는 본 작업의 backfill 채널 외에는 추가하지 않는다. backfill 메서드는 좁은 이름과 도메인 내부 가드(선교 외 호출 시 예외, area==null 예외, 이미 값 있을 시 idempotent no-op)로 다른 용도 사용을 차단한다.
- Mutable Rules: 향후 다른 영역에도 sub-area 식별이 필요하면 동일 필드를 재사용한다(필드 의미는 "course_offerings.area_code" 그대로이므로 확장 가능).
- Immutable Rules: `CourseDto`의 기존 필드(`year`, `courseName`, `credits`, `grade`, `semester`)와 `AreaProgressDto`의 모든 기존 필드 의미는 그대로 유지한다. CourseOffering의 기존 필드는 본 backfill 외 어떤 흐름에서도 변경되지 않는다.

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

- Scenario Name: 재스크래핑 시 historical NULL 선교 row 정합성 보강 (2차 작업)
  - Trigger: 사용자가 첫 연결 또는 재연결을 트리거하여 sync 흐름이 실행됨.
  - Actor: `SyncAcademicRecordService.processCurriculumData` → `CourseOfferingService.getOrCreateAll`
  - Steps:
    1. 기존 row reuse 분기에서 각 row에 대해 4개 가드(Rule 6) 검사.
    2. 모두 충족 시 `liberalArtsAreaCodeRepository.getReferenceById(cmd.areaCode())`로 프록시 로드.
    3. `existing.backfillMissionLiberalAreaCode(area)` 호출 — 도메인 메서드가 다시 한 번 내부 가드(선교 검증, idempotent, null 검증) 적용 후 할당.
    4. `@Transactional` dirty checking으로 자동 UPDATE flush.
  - Expected Result: 가드 통과 row만 `course_offerings.area_code`가 채워진다. 명시 save 호출 없음.

- Scenario Name: 재스크래핑 시 cmd.areaCode 가 null 또는 0
  - Condition: `extractLeadingDigit(cltTerrNm)`가 0을 반환했거나 portal 응답에 `cltTerrNm`이 없음.
  - Expected Behavior: 가드 (iii)/(iv)에 의해 backfill 건너뜀. 기존 NULL 그대로 유지.

- Scenario Name: 재스크래핑 시 이미 area_code가 채워진 선교 row
  - Condition: `existing.liberalArtsAreaCode != null`.
  - Expected Behavior: Service 가드 (ii)에서 1차 차단. 만약 도메인 메서드까지 도달해도 내부 idempotent 가드로 no-op.

- Scenario Name: 도메인 메서드를 비-선교 row에서 잘못 호출
  - Condition: 향후 코드 변경 실수로 `backfillMissionLiberalAreaCode`를 다른 영역의 CourseOffering에서 호출.
  - Expected Behavior: `IllegalStateException` 발생. 호출자가 즉시 실패를 인지하도록 fail-fast.

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
### 1차 작업 (응답 노출)
- [x] Phase 1 Spec fixed (이 문서 + clarify/plan/tasks 승인)
- [x] Phase 2 Domain complete — 영향 없음 (CourseOffering/LiberalArtsAreaCode/FacultyDivision 변경 없음)
- [x] Phase 3 Application complete — 영향 없음 (graduation 모듈은 application layer 분리가 없는 구조)
- [x] Phase 4 Infrastructure complete — `GraduationQueryRepository.getLatestValidCourses` SQL 컬럼 추가 + 매핑 변경, `toCourseResponseDto` 매퍼 변경 (단일전공·복수전공 흐름에 동시 반영), `CourseInternalDto` 필드 추가
- [x] Phase 5 Global/Config complete — 글로벌 Jackson `default-property-inclusion` 미설정 확인 → `CourseDto` 클래스 레벨 `@JsonInclude(NON_NULL)` 적용 (clarify Decision 5)
- [x] Phase 6 API/Controller complete — `CourseDto`에 `liberalAreaCode` 필드 + `@Schema(nullable = true)` + 클래스 레벨 `@JsonInclude(NON_NULL)` 적용. 컨트롤러 변경 없음. `openapi.yaml`은 graduation 스키마 미정의로 변경 불요

### 2차 작업 (정합성 보강 — 선교 area_code backfill)
- [x] Phase 2 Domain — `CourseOffering.backfillMissionLiberalAreaCode(LiberalArtsAreaCode)` 메서드 추가 (4겹 방어선: 좁은 이름 + 선교 검증 + idempotent + null 검증 + Javadoc)
- [x] Phase 3 Application — 영향 없음
- [x] Phase 4 Infrastructure — `CourseOfferingService.getOrCreateAll`의 기존 row reuse 분기 안에 `shouldBackfillMissionAreaCode` 가드 + 도메인 메서드 호출 추가
- [x] Phase 6 API/Controller — 영향 없음 (응답 shape 변경 없음, 다만 historical NULL row가 사용자 재연결 시점부터 점진적으로 정상화됨)
- [x] Phase 7 Tests — Service 5건 (T1~T5) + 도메인 가드 4건 PASS

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
- `src/main/java/com/chukchuk/haksa/domain/course/model/CourseOffering.java` (2차 작업)
  - Description: `backfillMissionLiberalAreaCode(LiberalArtsAreaCode)` 도메인 메서드 추가. 내부 가드 3종 + Javadoc으로 다른 용도 차단.
  - Layer: Domain
- `src/main/java/com/chukchuk/haksa/domain/course/service/CourseOfferingService.java` (2차 작업)
  - Description: `getOrCreateAll`의 기존 row reuse 분기에 backfill 호출 추가. `shouldBackfillMissionAreaCode` private 헬퍼로 가드 응집.
  - Layer: Service
- `src/test/java/com/chukchuk/haksa/domain/course/service/CourseOfferingServiceUnitTests.java` (2차 작업, 기존 확장)
  - Description: backfill 6 케이스 검증 (T1~T6).
  - Layer: Test
