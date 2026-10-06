# 편입생 졸업요건 부분 진단 및 지정과목 이수 현황 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 일반 재학생 졸업진단을 보존하면서 편입생에게 총 취득학점, 편입 인정학점, GPA, 외국어 인증, 지정과목 이수 현황과 수동 확인 사유를 제공한다.

**Architecture:** `GraduationService`가 저장된 편입 여부로 일반·편입 경로를 캐시 조회 전에 분리하고, `TransferGraduationAnalysisService`가 기존 누적 성적·수강·지정과목·외국어 인증 데이터를 조합한다. `DesignatedCourseEvaluator`는 한 번 조회한 수강 기록을 정규화해 지정과목 상태와 편입 인정학점을 계산하며, 편입생 응답은 오래된 로컬 캐시를 피하기 위해 캐시하지 않는다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Springdoc, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/tasks/339/design.md`

## Global Constraints

- 이슈 #339와 `docs/tasks/339/design.md`의 확정 정책을 기준으로 한다.
- 총 취득학점은 `StudentAcademicRecord.totalEarnedCredits`만 사용하고 편입 인정학점을 다시 더하지 않는다.
- 편입 인정학점은 전공·교양·일반선택 영역에 임의 배치하지 않는다.
- 지정과목의 원본 `point`는 취득학점에 직접 반영하지 않는다.
- 편입생에게 일반 재학생 영역별 졸업요건 계산을 호출하지 않는다.
- 편입생의 최종 졸업 가능 여부를 단정하지 않는다.
- 일반 재학생의 응답 필드, 영역별 계산과 학생별 캐시 동작을 유지한다.
- DB migration과 신규 외부 의존성은 추가하지 않는다.
- 새 Java 파일은 첫 줄에 역할을 설명하는 한 줄짜리 한국어 주석을 둔다.
- 공개 API 변경은 Springdoc 계약 테스트와 실행 중인 `/v3/api-docs`로 검증한다.

---

### Task 1: 일반·편입생 응답 타입 확장

**Files:**
- Create: `src/main/java/com/chukchuk/haksa/domain/graduation/dto/GraduationAnalysisType.java`
- Create: `src/main/java/com/chukchuk/haksa/domain/graduation/dto/GraduationAnalysisStatus.java`
- Create: `src/main/java/com/chukchuk/haksa/domain/graduation/dto/DesignatedCourseCompletionStatus.java`
- Create: `src/main/java/com/chukchuk/haksa/domain/graduation/dto/TransferManualReviewReason.java`
- Create: `src/main/java/com/chukchuk/haksa/domain/graduation/dto/DesignatedCourseProgressDto.java`
- Create: `src/main/java/com/chukchuk/haksa/domain/graduation/dto/TransferGraduationProgressDto.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/graduation/dto/GraduationProgressResponse.java`
- Create: `src/test/java/com/chukchuk/haksa/domain/graduation/dto/GraduationProgressResponseJsonTest.java`

**Interfaces:**
- Consumes: 기존 `GraduationProgressResponse(List<AreaProgressDto>, Boolean)` 생성 계약.
- Produces: `GraduationProgressResponse.forTransfer(TransferGraduationProgressDto, Boolean)`, `analysisType`, `analysisStatus`, nullable `transferProgress`.

- [ ] **Step 1: 일반·편입생 JSON 계약의 실패 테스트를 작성한다.**

`GraduationProgressResponseJsonTest`에서 기존 생성자는 일반 분석으로 직렬화되고 편입 전용 객체는 생략되는지 검증한다.

```java
GraduationProgressResponse regular = new GraduationProgressResponse(List.of(), true);

JsonNode json = objectMapper.valueToTree(regular);

assertThat(json.path("analysisType").asText()).isEqualTo("REGULAR");
assertThat(json.path("analysisStatus").asText()).isEqualTo("CALCULATED");
assertThat(json.has("transferProgress")).isFalse();
```

편입 정적 팩터리는 빈 영역 목록과 편입 객체를 제공해야 한다.

```java
GraduationProgressResponse transfer =
    GraduationProgressResponse.forTransfer(transferProgress(), null);

JsonNode json = objectMapper.valueToTree(transfer);

assertThat(json.path("analysisType").asText()).isEqualTo("TRANSFER");
assertThat(json.path("analysisStatus").asText())
    .isEqualTo("MANUAL_REVIEW_REQUIRED");
assertThat(json.path("graduationProgress")).isEmpty();
assertThat(json.path("transferProgress").path("requiredTotalCredits").asInt())
    .isEqualTo(130);
```

- [ ] **Step 2: DTO 집중 테스트가 신규 타입 부재로 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponseJsonTest --stacktrace --no-daemon
```

Expected: 신규 enum, DTO와 `forTransfer`가 없어 컴파일이 실패한다.

- [ ] **Step 3: 공개 응답 enum과 record DTO를 추가한다.**

enum 값은 다음으로 고정한다.

```java
public enum GraduationAnalysisType {
  REGULAR,
  TRANSFER
}

public enum GraduationAnalysisStatus {
  CALCULATED,
  MANUAL_REVIEW_REQUIRED
}

public enum DesignatedCourseCompletionStatus {
  COMPLETED,
  NOT_COMPLETED,
  UNKNOWN
}
```

수동 확인 사유는 설계 문서와 같은 값으로 선언한다.

```java
public enum TransferManualReviewReason {
  TRANSFER_ENTRY_GRADE_UNKNOWN,
  REGISTERED_SEMESTERS_NOT_VERIFIED,
  REQUIRED_COURSES_NOT_ASSESSABLE,
  ELECTIVE_RATIO_NOT_ASSESSABLE,
  MINOR_OR_LINKED_MAJOR_NOT_ASSESSABLE,
  GRADUATION_REVIEW_NOT_AVAILABLE,
  ACADEMIC_SUMMARY_INCOMPLETE
}
```

지정과목 응답은 원본 표시 정보와 판정 상태만 포함한다.

```java
public record DesignatedCourseProgressDto(
    String courseCode,
    String courseName,
    Integer credits,
    DesignatedCourseCompletionStatus status) {}
```

편입생 진행 응답은 다음 계약으로 선언한다.

```java
public record TransferGraduationProgressDto(
    int requiredTotalCredits,
    Integer totalEarnedCredits,
    Integer remainingCredits,
    Boolean creditsFulfilled,
    int recognizedTransferCredits,
    BigDecimal cumulativeGpa,
    BigDecimal requiredGpa,
    Boolean gpaFulfilled,
    Integer completedSemesters,
    boolean designatedCoursesNeedsRefresh,
    List<DesignatedCourseProgressDto> designatedCourses,
    boolean manualReviewRequired,
    List<TransferManualReviewReason> manualReviewReasons) {
  public TransferGraduationProgressDto {
    designatedCourses = List.copyOf(designatedCourses);
    manualReviewReasons = List.copyOf(manualReviewReasons);
  }
}
```

- [ ] **Step 4: 기존 응답 생성자를 보존하고 편입생 팩터리를 추가한다.**

`GraduationProgressResponse`의 기존 생성자는 `REGULAR`, `CALCULATED`를 설정한다. `transferProgress`에는 필드 단위 `@JsonInclude(JsonInclude.Include.NON_NULL)`를 적용한다.

```java
public static GraduationProgressResponse forTransfer(
    TransferGraduationProgressDto transferProgress, Boolean languageCertFulfilled) {
  return new GraduationProgressResponse(
      GraduationAnalysisType.TRANSFER,
      GraduationAnalysisStatus.MANUAL_REVIEW_REQUIRED,
      List.of(),
      languageCertFulfilled,
      transferProgress);
}
```

- [ ] **Step 5: DTO 집중 테스트를 다시 실행해 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponseJsonTest --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: 응답 계약 확장을 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/domain/graduation/dto src/test/java/com/chukchuk/haksa/domain/graduation/dto/GraduationProgressResponseJsonTest.java
git commit -m "339 feat: 편입생 졸업진단 응답 계약 추가"
```

### Task 2: 지정과목 이수 및 편입 인정학점 평가기 추가

**Files:**
- Modify: `src/main/java/com/chukchuk/haksa/domain/academic/record/repository/StudentCourseRepository.java`
- Create: `src/main/java/com/chukchuk/haksa/domain/graduation/policy/DesignatedCourseEvaluator.java`
- Create: `src/test/java/com/chukchuk/haksa/domain/graduation/policy/DesignatedCourseEvaluatorTest.java`
- Create: `src/test/java/com/chukchuk/haksa/domain/academic/record/repository/StudentCourseRepositoryFetchTest.java`

**Interfaces:**
- Consumes: `List<StudentDesignatedCourse>`와 과목·개설 정보가 fetch join된 `List<StudentCourse>`.
- Produces: `DesignatedCourseEvaluator.Evaluation(List<DesignatedCourseProgressDto>, int recognizedTransferCredits)`.

- [ ] **Step 1: 과목 코드 정규화와 유효 성적 경계의 실패 테스트를 작성한다.**

`DesignatedCourseEvaluatorTest`에서 다음 표를 parameterized test로 검증한다.

```java
@CsvSource({
  "A0, false, COMPLETED",
  "P, false, COMPLETED",
  "F, false, NOT_COMPLETED",
  "R, false, NOT_COMPLETED",
  "NP, false, NOT_COMPLETED",
  "IP, false, NOT_COMPLETED",
  "A0, true, NOT_COMPLETED"
})
```

`" abc123 "` 지정 코드는 `"ABC123"` 수강 코드와 일치해야 하고, blank 레거시 코드는 `UNKNOWN`이어야 한다. 지정과목의 순서와 중복은 결과에서도 유지해야 한다.

- [ ] **Step 2: 편입 인정학점 중복 방지의 실패 테스트를 작성한다.**

동일 코드의 유효한 15학점·18학점 기록과 다른 인정 코드의 17학점 기록을 주면 최대값만 코드별로 합산해야 한다.

```java
DesignatedCourseEvaluator.Evaluation result =
    evaluator.evaluate(List.of(), List.of(course("07045", 15), course("07045", 18), course("07050", 17)));

assertThat(result.recognizedTransferCredits()).isEqualTo(35);
```

인정 코드가 아닌 과목, `points=null`, `F/R/NP/IP`, 재수강 삭제 기록은 합계에서 제외한다.

- [ ] **Step 3: 평가기 테스트가 클래스 부재로 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.policy.DesignatedCourseEvaluatorTest --stacktrace --no-daemon
```

Expected: `DesignatedCourseEvaluator`가 없어 컴파일이 실패한다.

- [ ] **Step 4: 전체 수강 기록을 한 번에 읽는 fetch join 쿼리를 추가한다.**

`StudentCourseRepository`에 다음 메서드를 추가한다.

```java
@Query(
    """
      SELECT sc FROM StudentCourse sc
      JOIN FETCH sc.offering co
      JOIN FETCH co.course c
      WHERE sc.student.id = :studentId
    """)
List<StudentCourse> findAllWithCourseByStudentId(@Param("studentId") UUID studentId);
```

`StudentCourseRepositoryFetchTest`는 영속성 컨텍스트를 비운 뒤 결과의 `offering.course.courseCode`에 접근해 추가 lazy 조회 없이 데이터가 로드되는지 확인한다.

- [ ] **Step 5: 저장소 비의존 평가기를 최소 구현한다.**

평가기의 고정 인정 코드는 다음과 같다.

```java
private static final Set<String> TRANSFER_CREDIT_CODES =
    Set.of("07045", "07046", "00111", "07050");

private static final Set<GradeType> NON_PASSING_GRADES =
    EnumSet.of(GradeType.F, GradeType.R, GradeType.NP, GradeType.IP);
```

코드 정규화는 `trim().toUpperCase(Locale.ROOT)`를 사용한다. 수강 기록은 한 번 순회해 유효 과목 코드별 최대 학점 map을 만들고, 지정과목은 입력 순서대로 다음 결과로 변환한다.

```java
public record Evaluation(
    List<DesignatedCourseProgressDto> designatedCourses,
    int recognizedTransferCredits) {
  public Evaluation {
    designatedCourses = List.copyOf(designatedCourses);
  }
}
```

유효 수강 기록의 조건은 `grade != null`, 성적이 `NON_PASSING_GRADES`에 없고 `isRetakeDeleted=false`인 경우다. 지정과목의 `point`는 결과의 `credits`로만 복사하고 인정학점 계산에는 수강 기록의 `StudentCourse.points`만 사용한다.

- [ ] **Step 6: 평가기와 저장소 집중 테스트를 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.policy.DesignatedCourseEvaluatorTest --tests com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepositoryFetchTest --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: 평가 로직과 조회 계약을 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/domain/academic/record/repository/StudentCourseRepository.java src/main/java/com/chukchuk/haksa/domain/graduation/policy/DesignatedCourseEvaluator.java src/test/java/com/chukchuk/haksa/domain/graduation/policy/DesignatedCourseEvaluatorTest.java src/test/java/com/chukchuk/haksa/domain/academic/record/repository/StudentCourseRepositoryFetchTest.java
git commit -m "339 feat: 지정과목 이수 및 인정학점 평가 추가"
```

### Task 3: 편입생 부분 진단 서비스 추가

**Files:**
- Create: `src/main/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisService.java`
- Create: `src/test/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisServiceTests.java`

**Interfaces:**
- Consumes: `Student`, 누적 성적, 전체 수강 기록, 지정과목 스냅샷, 외국어 인증.
- Produces: `GraduationProgressResponse analyze(Student student)`.

- [ ] **Step 1: 총학점과 GPA 계산의 실패 테스트를 작성한다.**

누적 취득학점 112, GPA 3.2, 복수전공 없음이면 다음 결과를 기대한다.

```java
GraduationProgressResponse response = service.analyze(student);
TransferGraduationProgressDto progress = response.getTransferProgress();

assertThat(progress.requiredTotalCredits()).isEqualTo(130);
assertThat(progress.totalEarnedCredits()).isEqualTo(112);
assertThat(progress.remainingCredits()).isEqualTo(18);
assertThat(progress.creditsFulfilled()).isFalse();
assertThat(progress.requiredGpa()).isEqualByComparingTo("2.0");
assertThat(progress.gpaFulfilled()).isTrue();
```

130학점 이상이면 남은 학점은 0이고, 복수전공이 있으면 GPA 기준은 2.5여야 한다. 누적 취득학점 또는 GPA가 `null`이면 관련 충족 상태도 `null`이고 `ACADEMIC_SUMMARY_INCOMPLETE` 사유가 포함되어야 한다.

- [ ] **Step 2: 지정과목 스냅샷과 수동 확인 상태의 실패 테스트를 작성한다.**

스냅샷 버전이 없으면 `designatedCoursesNeedsRefresh=true`, 버전이 있고 목록이 비면 `false`여야 한다. 응답은 항상 설계 문서의 고정 수동 확인 사유를 포함하고 `manualReviewRequired=true`여야 한다.

외국어 인증 `Optional.empty()`, `true`, `false`는 기존 top-level `languageCertFulfilled`과 `languageCertNeedsRefresh` 계약을 유지해야 한다.

- [ ] **Step 3: 서비스 집중 테스트가 클래스 부재로 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.service.TransferGraduationAnalysisServiceTests --stacktrace --no-daemon
```

Expected: `TransferGraduationAnalysisService`가 없어 컴파일이 실패한다.

- [ ] **Step 4: 편입생 분석 서비스를 최소 구현한다.**

다음 의존성을 생성자 주입한다.

```java
private final StudentAcademicRecordService studentAcademicRecordService;
private final StudentCourseRepository studentCourseRepository;
private final StudentDesignatedCourseRepository studentDesignatedCourseRepository;
private final StudentGraduationProgressService studentGraduationProgressService;
private final DesignatedCourseEvaluator designatedCourseEvaluator;
```

서비스 상수는 다음과 같이 둔다.

```java
private static final int REQUIRED_TOTAL_CREDITS = 130;
private static final BigDecimal DEFAULT_REQUIRED_GPA = new BigDecimal("2.0");
private static final BigDecimal SECONDARY_MAJOR_REQUIRED_GPA = new BigDecimal("2.5");
```

`analyze(Student student)`는 각 저장소를 한 번씩 호출하고, `DesignatedCourseEvaluator.Evaluation`을 재사용해 지정과목과 인정학점을 함께 구성한다. `completedSemesters`는 응답 참고값으로 복사하지만 충족 여부를 계산하지 않는다.

- [ ] **Step 5: 서비스 집중 테스트를 다시 실행해 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.service.TransferGraduationAnalysisServiceTests --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: 편입생 분석 서비스를 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisService.java src/test/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisServiceTests.java
git commit -m "339 feat: 편입생 졸업요건 부분 진단 추가"
```

### Task 4: 졸업진단 분기 전환과 편입생 차단 제거

**Files:**
- Modify: `src/main/java/com/chukchuk/haksa/domain/graduation/service/GraduationService.java`
- Modify: `src/test/java/com/chukchuk/haksa/domain/graduation/service/GraduationServiceTests.java`

**Interfaces:**
- Consumes: `TransferGraduationAnalysisService.analyze(Student)`.
- Produces: 일반 재학생은 기존 경로, 편입생은 캐시를 우회한 편입 전용 응답.

- [ ] **Step 1: 일반·편입생 분기의 실패 테스트로 기존 차단 테스트를 교체한다.**

편입생 테스트는 전용 응답을 반환하고 다음 상호작용을 검증한다.

```java
when(transferGraduationAnalysisService.analyze(student)).thenReturn(transferResponse);

assertThat(graduationService.getGraduationProgress(STUDENT_ID)).isSameAs(transferResponse);
verify(transferGraduationAnalysisService).analyze(student);
verifyNoInteractions(academicCache, graduationMajorResolver, graduationQueryRepository);
```

일반 재학생 테스트는 `transferGraduationAnalysisService`와 상호작용하지 않고 기존 캐시 조회·저장 및 영역별 계산을 유지해야 한다.

- [ ] **Step 2: 분기 테스트가 기존 예외 때문에 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.service.GraduationServiceTests --stacktrace --no-daemon
```

Expected: 편입생이 `TRANSFER_STUDENT_UNSUPPORTED`로 차단되어 실패한다.

- [ ] **Step 3: 캐시보다 앞에 편입생 전용 분기를 추가한다.**

`GraduationService.getGraduationProgress`의 학생 조회 직후에 다음 분기를 둔다.

```java
Student student = studentService.getStudentById(studentId);
if (student.isTransferStudent()) {
  return transferGraduationAnalysisService.analyze(student);
}
```

`validateTransferStudent` 메서드와 `TRANSFER_STUDENT_UNSUPPORTED` 사용을 제거한다. 일반 재학생 코드는 분기 아래에서 기존 순서와 동작을 유지한다.

- [ ] **Step 4: 서비스 분기 테스트를 다시 실행해 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.service.GraduationServiceTests --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: 분기 전환을 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/domain/graduation/service/GraduationService.java src/test/java/com/chukchuk/haksa/domain/graduation/service/GraduationServiceTests.java
git commit -m "339 feat: 편입생 졸업진단 전용 분기 연결"
```

### Task 5: 공개 API와 Springdoc 계약 갱신

**Files:**
- Modify: `src/main/java/com/chukchuk/haksa/domain/graduation/controller/docs/GraduationControllerDocs.java`
- Modify: `src/test/java/com/chukchuk/haksa/domain/graduation/controller/GraduationControllerApiIntegrationTest.java`
- Modify: `src/test/java/com/chukchuk/haksa/global/config/OpenApiResponseContractTest.java`

**Interfaces:**
- Consumes: Task 1의 일반·편입생 `GraduationProgressResponse`.
- Produces: 기존 endpoint에서 하위 호환 가능한 JSON과 Springdoc schema.

- [ ] **Step 1: 편입생 controller JSON 계약의 실패 테스트를 작성한다.**

`GraduationControllerApiIntegrationTest`에 편입 응답을 stub하고 다음 필드를 검증한다.

```java
.andExpect(jsonPath("$.data.analysisType").value("TRANSFER"))
.andExpect(jsonPath("$.data.analysisStatus").value("MANUAL_REVIEW_REQUIRED"))
.andExpect(jsonPath("$.data.graduationProgress").isEmpty())
.andExpect(jsonPath("$.data.transferProgress.totalEarnedCredits").value(112))
.andExpect(jsonPath("$.data.transferProgress.designatedCourses[0].status").value("COMPLETED"))
.andExpect(jsonPath("$.data.transferProgress.manualReviewRequired").value(true));
```

기존 일반 응답 테스트에는 `analysisType=REGULAR`, `analysisStatus=CALCULATED`, `transferProgress` 미노출 assertion을 추가한다.

- [ ] **Step 2: OpenAPI 스키마 실패 테스트를 작성한다.**

`OpenApiResponseContractTest`에서 `GraduationProgressResponse.transferProgress`가 `TransferGraduationProgressDto`를 참조하고 신규 enum 값이 문서화되는지 확인한다.

```java
assertThat(propertySchema(apiDocs, graduationData, "analysisType").path("enum").toString())
    .contains("REGULAR", "TRANSFER");
assertThat(propertySchema(apiDocs, graduationData, "analysisStatus").path("enum").toString())
    .contains("CALCULATED", "MANUAL_REVIEW_REQUIRED");
```

- [ ] **Step 3: API 집중 테스트가 문서와 계약 부재로 실패하는지 확인한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.controller.GraduationControllerApiIntegrationTest --tests com.chukchuk.haksa.global.config.OpenApiResponseContractTest --stacktrace --no-daemon
```

Expected: 편입 응답 fixture와 신규 schema assertion이 실패한다.

- [ ] **Step 4: Springdoc 설명과 schema annotation을 갱신한다.**

`GraduationControllerDocs` 설명에 다음 계약을 명시한다.

```text
일반 재학생은 graduationProgress로 영역별 진행률을 반환합니다. 편입생은 일반 영역별 계산을 적용하지 않고 transferProgress로 총 취득학점, 인정학점, GPA, 지정과목 이수 현황과 수동 확인 사유를 반환합니다.
```

신규 enum과 DTO record 구성요소에 `@Schema` 설명과 nullable 여부를 명시한다.

- [ ] **Step 5: API 집중 테스트를 다시 실행해 통과시킨다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.domain.graduation.controller.GraduationControllerApiIntegrationTest --tests com.chukchuk.haksa.global.config.OpenApiResponseContractTest --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: API 계약 변경을 커밋한다.**

```bash
git add src/main/java/com/chukchuk/haksa/domain/graduation/controller/docs/GraduationControllerDocs.java src/main/java/com/chukchuk/haksa/domain/graduation/dto src/test/java/com/chukchuk/haksa/domain/graduation/controller/GraduationControllerApiIntegrationTest.java src/test/java/com/chukchuk/haksa/global/config/OpenApiResponseContractTest.java
git commit -m "339 docs: 편입생 졸업진단 API 계약 문서화"
```

### Task 6: 실제 포털 계약과 편입생 분석 통합 회귀 테스트

**Files:**
- Create: `src/test/resources/fixtures/portal/transfer-graduation.json`
- Modify: `src/test/java/com/chukchuk/haksa/infrastructure/portal/mapper/PortalDataMapperTests.java`
- Create: `src/test/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisIntegrationTest.java`

**Interfaces:**
- Consumes: 실제 스크래퍼 형태의 `point`, `gainPoint`, `flangPassGb`, `designatedCourses` JSON과 저장된 학사 데이터.
- Produces: 포털 입력부터 편입생 진단 데이터까지의 회귀 근거.

- [ ] **Step 1: 실제 포털 형태 fixture를 추가한다.**

fixture에는 다음 데이터를 포함한다.

```json
{
  "studentInfo": {
    "sno": "17019013",
    "studNm": "홍길동",
    "univCd": "01",
    "univNm": "수원대학교",
    "dpmjCd": "D1",
    "dpmjNm": "컴퓨터학부",
    "mjorCd": "M1",
    "mjorNm": "컴퓨터학과",
    "scrgStatNm": "재학",
    "enscYear": "2024",
    "enscSmrCd": "10",
    "enscDvcd": "2",
    "studGrde": 3,
    "facSmrCnt": 3,
    "flangPassGb": "통과"
  },
  "semesters": [
    {
      "semester": "2024-1",
      "courses": [
        {
          "subjtCd": "07045",
          "subjtNm": "편입인정학점",
          "point": null,
          "gainPoint": 65,
          "cretGrdCd": "P",
          "refacYearSmr": "-",
          "facDvnm": "일선",
          "subjtEstbSmrCd": "1",
          "subjtEstbYearSmr": "2024-1",
          "cretDelCd": null,
          "cretDelNm": null
        },
        {
          "subjtCd": "C101",
          "subjtNm": "자료구조",
          "point": 3,
          "gainPoint": 3,
          "cretGrdCd": "A0",
          "refacYearSmr": "-",
          "facDvnm": "전선",
          "subjtEstbSmrCd": "1",
          "subjtEstbYearSmr": "2024-1",
          "cretDelCd": null,
          "cretDelNm": null
        }
      ]
    }
  ],
  "academicRecords": {
    "listSmrCretSumTabYearSmr": [],
    "selectSmrCretSumTabSjTotal": {
      "gainPoint": "112",
      "applPoint": "112",
      "gainAvmk": "3.2",
      "gainTavgPont": "85"
    }
  },
  "designatedCourses": [
    {"orgClsCd": "20", "subjtCd": "C101", "subjtNm": "자료구조", "point": 3, "sno": "17019013"},
    {"orgClsCd": "20", "subjtCd": "C202", "subjtNm": "운영체제", "point": 3, "sno": "17019013"}
  ]
}
```

- [ ] **Step 2: mapper 회귀 테스트를 추가하고 실패를 확인한다.**

`PortalDataMapperTests`는 편입 여부, 인정과목의 65학점 fallback, 외국어 통과, 지정과목 두 행을 검증한다.

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --stacktrace --no-daemon
```

Expected: 기존 mapper 계약이 유지되므로 `BUILD SUCCESSFUL`. 실패하면 실제 assertion과 stack trace로 회귀 원인을 확인하고 #339 범위에서 깨진 계약만 수정한다.

- [ ] **Step 3: 편입생 분석 통합 테스트를 작성한다.**

PostgreSQL 통합 테스트에 편입생, 누적 112학점·GPA 3.2, 65학점 인정과목, 유효하게 이수한 `C101`, 미이수 지정과목 `C202`를 저장한다. 서비스 호출 결과에서 다음을 검증한다.

```java
assertThat(progress.totalEarnedCredits()).isEqualTo(112);
assertThat(progress.recognizedTransferCredits()).isEqualTo(65);
assertThat(progress.totalEarnedCredits()).isNotEqualTo(177);
assertThat(progress.designatedCourses())
    .extracting(DesignatedCourseProgressDto::status)
    .containsExactly(COMPLETED, NOT_COMPLETED);
```

별도 케이스에서 최신 지정과목 스냅샷이 빈 배열인 상태는 빈 목록과 `designatedCoursesNeedsRefresh=false`, 스냅샷 미수신 상태는 `true`인지 확인한다.

- [ ] **Step 4: 통합 집중 테스트를 실행한다.**

Run:

```bash
./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --tests com.chukchuk.haksa.domain.graduation.service.TransferGraduationAnalysisIntegrationTest --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: 포털 계약과 통합 회귀 테스트를 커밋한다.**

```bash
git add src/test/resources/fixtures/portal/transfer-graduation.json src/test/java/com/chukchuk/haksa/infrastructure/portal/mapper/PortalDataMapperTests.java src/test/java/com/chukchuk/haksa/domain/graduation/service/TransferGraduationAnalysisIntegrationTest.java
git commit -m "339 test: 편입생 졸업진단 통합 계약 보호"
```

### Task 7: 문서와 전체 검증

**Files:**
- Review: `docs/tasks/339/design.md` and record every implementation deviation found during verification.
- Modify: `docs/tasks/339/plan.md` to record completed checks and deviations.
- Modify in Wiki repository: 졸업요건 및 포털 데이터 계약 관련 문서.

**Interfaces:**
- Consumes: Task 1~6의 구현과 테스트 결과.
- Produces: 검증 가능한 구현 상태, 최신 Springdoc와 Wiki 계약.

- [ ] **Step 1: Java 자동 포맷을 적용하고 정적 검사를 실행한다.**

Run:

```bash
./gradlew spotlessApply --no-daemon
./gradlew spotlessCheck --no-daemon
./gradlew checkstyleMain checkstyleTest --no-daemon
```

Expected: 모든 명령이 `BUILD SUCCESSFUL`.

- [ ] **Step 2: 졸업진단과 포털 집중 테스트를 실행한다.**

Run:

```bash
./gradlew test --tests 'com.chukchuk.haksa.domain.graduation.*' --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: 전체 통합 검증을 실행한다.**

Run:

```bash
./gradlew check --stacktrace --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: 실행 중인 애플리케이션의 OpenAPI 문서를 검증한다.**

로컬 설정으로 애플리케이션을 실행한 뒤 `/v3/api-docs`를 조회한다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local' --no-daemon
curl --fail --silent http://localhost:8080/v3/api-docs
```

응답에서 `GraduationAnalysisType`, `GraduationAnalysisStatus`, `TransferGraduationProgressDto`, `DesignatedCourseProgressDto` schema와 `/api/graduation/progress`의 200 응답 참조를 확인한다.

- [ ] **Step 5: Wiki 계약을 갱신한다.**

별도 `cchaksa-backend.wiki` 저장소의 `master` 브랜치에서 졸업요건 문서와 포털 데이터 계약 문서에 다음을 기록한다.

- 편입생은 부분 진단만 제공한다.
- 총 취득학점은 포털 누적 취득학점이 단일 기준이다.
- 편입 인정학점과 지정과목 학점은 중복 합산하지 않는다.
- 지정과목 상태와 수동 확인 사유 enum 계약.
- 자동 판정에서 제외된 편입생 요건.

- [ ] **Step 6: diff와 migration 부재를 확인한다.**

Run:

```bash
git diff --check origin/dev...HEAD
git diff --name-only origin/dev...HEAD
```

Expected: whitespace 오류가 없고 `src/main/resources/db/migration` 변경이 없다.

- [ ] **Step 7: 검증 결과와 남은 위험을 계획 문서에 기록한다.**

`docs/tasks/339/plan.md` 하단 `Verification`에 실제 실행 명령과 결과를 기록한다. 130학점 예외 학과, 등록학기 의미, 필수과목·전선 비율·졸업심사 데이터 부재를 남은 위험으로 유지한다.

- [ ] **Step 8: 문서와 최종 검증 결과를 커밋한다.**

```bash
git add docs/tasks/339
git commit -m "339 docs: 편입생 졸업진단 검증 결과 기록"
```

## Verification

- 구현 완료 기준은 Task 1~6의 코드·테스트·API 계약 반영으로 충족했다.
- `./gradlew test --tests com.chukchuk.haksa.domain.graduation.controller.GraduationControllerApiIntegrationTest --tests com.chukchuk.haksa.global.config.OpenApiResponseContractTest --stacktrace --no-daemon` → `BUILD SUCCESSFUL`.
- `./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --tests com.chukchuk.haksa.domain.graduation.service.TransferGraduationAnalysisIntegrationTest --stacktrace --no-daemon` → `BUILD SUCCESSFUL`.
- `./gradlew spotlessCheck checkstyleMain checkstyleTest --no-daemon` → `BUILD SUCCESSFUL`.
- `./gradlew test --tests 'com.chukchuk.haksa.domain.graduation.*' --tests com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapperTests --stacktrace --no-daemon` → `BUILD SUCCESSFUL`.
- `./gradlew check --stacktrace --no-daemon` → `BUILD SUCCESSFUL`.
- `git diff --check origin/dev...HEAD` → whitespace 오류 없음. 변경 파일에 `src/main/resources/db/migration`이 없어 이번 이슈에서는 새 migration을 추가하지 않았다.
- OpenAPI는 `OpenApiResponseContractTest`에서 `/api/graduation/progress`의 신규 schema와 참조를 검증했다. `bootRun`/jar를 통한 별도 서버 검증은 테스트 프로필의 H2가 production runtime classpath에 없고 local 프로필은 개인 PostgreSQL·환경변수를 요구해 실행하지 못했다.
- CodeRabbit 리뷰 반영 후 `./gradlew test --tests com.chukchuk.haksa.domain.graduation.service.TransferGraduationAnalysisServiceTests --tests com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepositoryFetchTest --tests com.chukchuk.haksa.domain.graduation.service.TransferGraduationAnalysisIntegrationTest --no-daemon`을 Java 17 환경에서 실행해 `BUILD SUCCESSFUL`을 확인했다.
- CodeRabbit 리뷰 반영 후 `./gradlew spotlessCheck checkstyleMain checkstyleTest --no-daemon`과 `./gradlew check --stacktrace --no-daemon`을 Java 17 환경에서 실행해 모두 `BUILD SUCCESSFUL`을 확인했다.
- Wiki `master`에 `16fac8d` (`339 docs: 편입생 졸업진단 계약 문서화`)를 반영했다.
- 남은 위험은 130학점 예외 학과, `completedSemesters`의 규정상 의미, 편입학년 이후 필수과목·전공선택 비율·부전공/연계전공·졸업심사 데이터 부재다. 이 항목들은 `MANUAL_REVIEW_REQUIRED`와 사유 enum으로 노출하며 자동 졸업 확정에서 제외했다.
