// 실행 서버에서 편입 테스트 데이터 수정과 실제 졸업진단 및 초기화를 검증한다.

package com.chukchuk.haksa.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.application.portal.SyncDesignatedCourseService;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.graduation.model.StudentGraduationProgress;
import com.chukchuk.haksa.domain.graduation.repository.StudentGraduationProgressRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:admin-transfer-349;MODE=PostgreSQL;"
          + "DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR,SEMESTER",
      "scraping.scheduler.enabled=false",
      "scraping.publisher.enabled=false",
      "scraping.stale.enabled=false",
      "security.jwt.access-expiration=3600000"
    })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminTransferDataHttpIntegrationTest {
  private static final String TRANSFER_PATH = "/api/admin/me/transfer-data";
  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort private int port;
  @Autowired private ObjectMapper mapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DepartmentRepository departments;
  @Autowired private StudentRepository students;
  @Autowired private StudentAcademicRecordRepository records;
  @Autowired private StudentGraduationProgressRepository graduationProgress;
  @Autowired private StudentDesignatedCourseRepository designatedCourses;
  @Autowired private StudentCourseRepository studentCourses;
  @Autowired private CourseRepository courses;
  @Autowired private CourseOfferingRepository offerings;
  @Autowired private SyncDesignatedCourseService syncDesignatedCourses;
  @Autowired private AcademicCache cache;

  @Test
  void patchesInputsThroughExistingCalculationAndResetsThem() throws Exception {
    Account account = createAccount();
    assertThat(progress(account).path("analysisType").asText()).isEqualTo("REGULAR");
    assertThat(cache.getGraduationProgress(account.studentId())).isNotNull();
    JsonNode core = createCourse(account, "CORE349", "전핵", 10);
    createCourse(account, "ELECTIVE349", "전선", 23);
    Course recognized = courses.save(new Course("07045", "편입인정학점"));
    CourseOffering offering =
        offerings.save(
            new CourseOffering(
                20261,
                false,
                2026,
                10,
                null,
                "A",
                null,
                null,
                65,
                null,
                FacultyDivision.일선,
                recognized,
                null,
                null,
                null));
    ok(
        "PATCH",
        "/api/admin/me/graduation-courses",
        mapper.writeValueAsString(
            Map.of("area", "일선", "addOfferingIds", List.of(offering.getId()), "grade", "P")),
        account.token());
    String code = core.path("courseCode").asText();
    patch(
        account,
        """
        {"isTransferStudent":true,"totalEarnedCredits":112,"cumulativeGpa":3.2,
         "completedSemesters":3,"languageCertFulfilled":true,
         "designatedCourses":[{"orgClsCd":"20","subjtCd":"%s","point":99},
                              {"orgClsCd":"20","subjtCd":"MISSING349","point":3}]}
        """
            .formatted(code));

    assertThat(cache.getGraduationProgress(account.studentId())).isNull();
    JsonNode data = progress(account);
    assertThat(data.path("analysisType").asText()).isEqualTo("TRANSFER");
    assertThat(data.path("languageCertFulfilled").asBoolean()).isTrue();
    JsonNode transfer = data.path("transferProgress");
    assertThat(transfer.path("totalEarnedCredits").asInt()).isEqualTo(112);
    assertThat(transfer.path("remainingCredits").asInt()).isEqualTo(18);
    assertThat(transfer.path("recognizedTransferCredits").asInt()).isEqualTo(65);
    assertThat(transfer.path("cumulativeGpa").decimalValue()).isEqualByComparingTo("3.2");
    assertThat(transfer.path("gpaFulfilled").asBoolean()).isTrue();
    assertThat(transfer.path("completedSemesters").asInt()).isEqualTo(3);
    assertThat(transfer.path("designatedCoursesNeedsRefresh").asBoolean()).isFalse();
    assertThat(transfer.path("designatedEarnedCredits").asInt()).isEqualTo(10);
    assertThat(transfer.path("designatedCourses").get(0).path("status").asText())
        .isEqualTo("COMPLETED");
    assertThat(transfer.path("designatedCourses").get(1).path("status").asText())
        .isEqualTo("NOT_COMPLETED");
    JsonNode coreArea = transfer.path("areas").get(0);
    assertThat(coreArea.path("requiredCredits").decimalValue()).isEqualByComparingTo("9.5");
    assertThat(coreArea.path("earnedCredits").asInt()).isEqualTo(10);
    assertThat(coreArea.path("fulfilled").asBoolean()).isTrue();
    assertThat(transfer.path("areas").get(1).path("requiredCredits").decimalValue())
        .isEqualByComparingTo("22.5");

    final Instant snapshot = student(account).getDesignatedCoursesSnapshotVersion();
    Department other = departments.save(new Department("other349", "다른 전공"));
    ok(
        "PATCH",
        "/api/admin/me/major",
        "{\"majorDepartmentId\":" + other.getId() + ",\"dualMajorEnabled\":false}",
        account.token());
    ok("POST", "/api/admin/me/reset", "{}", account.token());
    Student reset = student(account);
    assertThat(reset.isTransferStudent()).isFalse();
    assertThat(reset.getAdmissionType()).isEqualTo("신입");
    assertThat(reset.getAcademicInfo().getCompletedSemesters()).isZero();
    assertThat(reset.getAcademicInfo().getAdmissionYear()).isEqualTo(2026);
    assertThat(reset.getMajor().getId()).isEqualTo(account.departmentId());
    assertThat(reset.getSecondaryMajor()).isNull();
    assertThat(reset.getDesignatedCoursesSnapshotVersion()).isNull();
    assertThat(reset.getDesignatedCoursesResetAt()).isAfter(snapshot);
    assertThat(records.findByStudentId(account.studentId())).isEmpty();
    assertThat(
            graduationProgress
                .findByStudentId(account.studentId())
                .orElseThrow()
                .getLanguageCertFulfilled())
        .isNull();
    assertThat(designatedCourses.findAllByStudentIdOrderBySourceOrder(account.studentId()))
        .isEmpty();
    assertThat(studentCourses.findAllWithCourseByStudentId(account.studentId())).isEmpty();
    assertThat(progress(account).path("analysisType").asText()).isEqualTo("REGULAR");

    patch(account, "{\"isTransferStudent\":true}");
    JsonNode cleared = progress(account).path("transferProgress");
    assertThat(cleared.path("totalEarnedCredits").isNull()).isTrue();
    assertThat(cleared.path("cumulativeGpa").isNull()).isTrue();
    assertThat(cleared.path("designatedCoursesNeedsRefresh").asBoolean()).isTrue();
    patch(account, "{\"designatedCourses\":[]}");
    assertThat(
            progress(account)
                .path("transferProgress")
                .path("designatedCoursesNeedsRefresh")
                .asBoolean())
        .isFalse();
  }

  @Test
  void preservesOmittedFieldsAndAcceptsZeroFalseAndEmptyReplacement() throws Exception {
    Account account = createAccount();
    patch(account, "{\"cumulativeGpa\":4.5}");
    StudentAcademicRecord first = records.findByStudentId(account.studentId()).orElseThrow();
    assertThat(first.getTotalEarnedCredits()).isNull();
    assertThat(first.getTotalAttemptedCredits()).isNull();
    jdbc.update(
        "UPDATE student_academic_records SET total_attempted_credits=120, percentile=87 "
            + "WHERE student_id=?",
        account.studentId());
    patch(
        account,
        """
        {"isTransferStudent":true,"totalEarnedCredits":112,"completedSemesters":3,
         "languageCertFulfilled":true,"designatedCourses":[{"orgClsCd":"20","subjtCd":"A"}]}
        """);
    final Instant snapshot = student(account).getDesignatedCoursesSnapshotVersion();
    patch(account, "{\"totalEarnedCredits\":0}");
    patch(account, "{}");
    StudentAcademicRecord partial = records.findByStudentId(account.studentId()).orElseThrow();
    assertThat(partial.getId()).isEqualTo(first.getId());
    assertThat(partial.getTotalEarnedCredits()).isZero();
    assertThat(partial.getCumulativeGpa()).isEqualByComparingTo("4.5");
    assertThat(partial.getTotalAttemptedCredits()).isEqualTo(120);
    assertThat(partial.getPercentile()).isEqualByComparingTo("87");
    assertThat(student(account).isTransferStudent()).isTrue();
    assertThat(student(account).getAcademicInfo().getCompletedSemesters()).isEqualTo(3);
    assertThat(student(account).getDesignatedCoursesSnapshotVersion()).isEqualTo(snapshot);
    assertThat(designatedCourses.findAllByStudentIdOrderBySourceOrder(account.studentId()))
        .hasSize(1);
    patch(
        account,
        """
        {"isTransferStudent":false,"completedSemesters":0,"cumulativeGpa":0,
         "languageCertFulfilled":false,"designatedCourses":[]}
        """);
    assertThat(student(account).isTransferStudent()).isFalse();
    assertThat(student(account).getAdmissionType()).isEqualTo("신입");
    assertThat(student(account).getAcademicInfo().getCompletedSemesters()).isZero();
    assertThat(records.findByStudentId(account.studentId()).orElseThrow().getCumulativeGpa())
        .isZero();
    assertThat(
            graduationProgress
                .findByStudentId(account.studentId())
                .orElseThrow()
                .getLanguageCertFulfilled())
        .isFalse();
    assertThat(designatedCourses.findAllByStudentIdOrderBySourceOrder(account.studentId()))
        .isEmpty();
    assertThat(student(account).getDesignatedCoursesSnapshotVersion()).isAfter(snapshot);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"isTransferStudent\":null",
        "\"totalEarnedCredits\":null",
        "\"cumulativeGpa\":null",
        "\"completedSemesters\":null",
        "\"languageCertFulfilled\":null",
        "\"designatedCourses\":null",
        "\"totalEarnedCredits\":-1",
        "\"completedSemesters\":-1",
        "\"cumulativeGpa\":4.51",
        "\"cumulativeGpa\":-0.01",
        "\"cumulativeGpa\":3.141",
        "\"designatedCourses\":[null]",
        "\"designatedCourses\":[{\"subjtCd\":\"A\"}]",
        "\"designatedCourses\":[{\"orgClsCd\":\"20\",\"subjtCd\":\" \"}]",
        "\"designatedCourses\":[{\"orgClsCd\":\"20\",\"subjtCd\":\"A\",\"point\":-1}]"
      })
  void invalidPayloadLeavesExistingDataUnchanged(String invalidField) throws Exception {
    Account account = createAccount();
    patch(account, "{\"isTransferStudent\":true,\"totalEarnedCredits\":112}");
    var rejected =
        request(
            "PATCH",
            TRANSFER_PATH,
            "{\"languageCertFulfilled\":true," + invalidField + "}",
            account.token());
    assertThat(rejected.statusCode()).isEqualTo(400);
    assertThat(records.findByStudentId(account.studentId()).orElseThrow().getTotalEarnedCredits())
        .isEqualTo(112);
    assertThat(student(account).isTransferStudent()).isTrue();
    assertThat(graduationProgress.findByStudentId(account.studentId())).isEmpty();
  }

  @Test
  void restrictsPatchToAuthenticatedTestAccountAndKeepsOrdinaryResetScope() throws Exception {
    final Account target = createAccount();
    assertThat(request("PATCH", TRANSFER_PATH, "{}", null).statusCode()).isEqualTo(401);
    Account ordinary = createAccount();
    jdbc.update(
        "UPDATE students SET student_code=? WHERE student_id=?",
        "2026349001",
        ordinary.studentId());
    assertThat(
            request("PATCH", TRANSFER_PATH, "{\"isTransferStudent\":true}", ordinary.token())
                .statusCode())
        .isEqualTo(403);
    records.save(new StudentAcademicRecord(student(ordinary), 10, 7, new BigDecimal("3.0"), null));
    ok("POST", "/api/admin/me/reset", "{}", ordinary.token());
    assertThat(records.findByStudentId(ordinary.studentId()).orElseThrow().getTotalEarnedCredits())
        .isEqualTo(7);
    assertThat(student(target).isTransferStudent()).isFalse();
  }

  @Test
  void rejectsMismatchedTestEmailAndDoesNotUseClientStudentId() throws Exception {
    Account account = createAccount();
    Account other = createAccount();
    patch(account, "{\"isTransferStudent\":true,\"studentId\":\"" + other.studentId() + "\"}");
    assertThat(student(account).isTransferStudent()).isTrue();
    assertThat(student(other).isTransferStudent()).isFalse();
    jdbc.update(
        "UPDATE users SET email=? WHERE id=?", "test_mismatch@cchaksa.dev", account.userId());
    assertThat(request("PATCH", TRANSFER_PATH, "{}", account.token()).statusCode()).isEqualTo(403);
  }

  @Test
  void resetBlocksOlderSnapshotsAndPreservesOtherGraduationState() throws Exception {
    Account account = createAccount();
    patch(account, "{\"languageCertFulfilled\":true,\"designatedCourses\":[]}");
    StudentGraduationProgress state =
        graduationProgress.findByStudentId(account.studentId()).orElseThrow();
    state.updateGraduationReview(true);
    graduationProgress.save(state);
    Instant version = student(account).getDesignatedCoursesSnapshotVersion();
    ok("POST", "/api/admin/me/reset", "{}", account.token());
    syncDesignatedCourses.sync(
        account.userId(),
        DesignatedCourseSnapshot.received(
            List.of(new DesignatedCourseData("20", "OLD", null, 3, null, null, null, null, 0))),
        version);
    assertThat(designatedCourses.findAllByStudentIdOrderBySourceOrder(account.studentId()))
        .isEmpty();
    assertThat(student(account).getDesignatedCoursesSnapshotVersion()).isNull();
    StudentGraduationProgress reset =
        graduationProgress.findByStudentId(account.studentId()).orElseThrow();
    assertThat(reset.getLanguageCertFulfilled()).isNull();
    assertThat(reset.getGraduationReviewFulfilled()).isTrue();
  }

  @Test
  void rollsBackAllInputsWhenDesignatedCoursePersistenceFails() throws Exception {
    Account account = createAccount();
    patch(
        account,
        """
        {"totalEarnedCredits":10,"languageCertFulfilled":false,
         "designatedCourses":[{"orgClsCd":"20","subjtCd":"ORIGINAL349"}]}
        """);
    Instant version = student(account).getDesignatedCoursesSnapshotVersion();
    jdbc.execute(
        "ALTER TABLE student_designated_courses ADD CONSTRAINT reject_test_course_349 "
            + "CHECK (subjt_cd <> 'FAIL349')");
    try {
      var response =
          request(
              "PATCH",
              TRANSFER_PATH,
              """
              {"isTransferStudent":true,"totalEarnedCredits":112,"languageCertFulfilled":true,
               "designatedCourses":[{"orgClsCd":"20","subjtCd":"FAIL349"}]}
              """,
              account.token());
      assertThat(response.statusCode()).isEqualTo(500);
      assertThat(student(account).isTransferStudent()).isFalse();
      assertThat(records.findByStudentId(account.studentId()).orElseThrow().getTotalEarnedCredits())
          .isEqualTo(10);
      assertThat(
              graduationProgress
                  .findByStudentId(account.studentId())
                  .orElseThrow()
                  .getLanguageCertFulfilled())
          .isFalse();
      assertThat(designatedCourses.findAllByStudentIdOrderBySourceOrder(account.studentId()))
          .extracting(course -> course.getSubjtCd())
          .containsExactly("ORIGINAL349");
      assertThat(student(account).getDesignatedCoursesSnapshotVersion()).isEqualTo(version);
    } finally {
      jdbc.execute("ALTER TABLE student_designated_courses DROP CONSTRAINT reject_test_course_349");
    }
    patch(
        account, "{\"designatedCourses\":[{\"orgClsCd\":\"20\",\"subjtCd\":\"REPLACEMENT349\"}]}");
    assertThat(designatedCourses.findAllByStudentIdOrderBySourceOrder(account.studentId()))
        .extracting(course -> course.getSubjtCd())
        .containsExactly("REPLACEMENT349");
  }

  @Test
  void exposesOptionalTypedFieldsAndAuthenticationInRunningOpenApi() throws Exception {
    JsonNode docs = ok("GET", "/v3/api-docs", null, null);
    JsonNode operation = docs.path("paths").path(TRANSFER_PATH).path("patch");
    assertThat(operation.path("security").toString()).contains("bearerAuth");
    assertThat(operation.path("responses").has("400")).isTrue();
    assertThat(operation.path("responses").has("401")).isTrue();
    assertThat(operation.path("responses").has("403")).isTrue();
    JsonNode schema = docs.path("components").path("schemas").path("UpdateTransferDataRequest");
    assertThat(schema.path("properties").size()).isEqualTo(6);
    assertThat(schema.path("required").isMissingNode() || schema.path("required").isEmpty())
        .isTrue();
    assertThat(schema.path("properties").path("isTransferStudent").path("type").asText())
        .isEqualTo("boolean");
    assertThat(schema.path("properties").path("designatedCourses").path("type").asText())
        .isEqualTo("array");
  }

  private Account createAccount() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Department department = departments.save(new Department(suffix, "테스트학과-" + suffix));
    for (int year : List.of(2024, 2026)) {
      for (var requirement : Map.of("전핵", 19, "전선", 45).entrySet()) {
        jdbc.update(
            "INSERT INTO department_area_requirements "
                + "(id, department_id, admission_year, area_type, required_credits) "
                + "VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID(),
            department.getId(),
            year,
            requirement.getKey(),
            requirement.getValue());
      }
    }
    JsonNode data =
        ok(
                "POST",
                "/api/admin/test-users",
                mapper.writeValueAsString(
                    Map.of("departmentId", department.getId(), "admissionYear", 2026)),
                null)
            .path("data");
    return new Account(
        UUID.fromString(data.path("userId").asText()),
        UUID.fromString(data.path("studentId").asText()),
        department.getId(),
        data.path("accessToken").asText());
  }

  private JsonNode createCourse(Account account, String code, String area, int credits)
      throws Exception {
    return ok(
            "POST",
            "/api/admin/me/test-courses",
            mapper.writeValueAsString(
                Map.of(
                    "courseCode",
                    code,
                    "area",
                    area,
                    "credits",
                    credits,
                    "year",
                    2026,
                    "semester",
                    10,
                    "grade",
                    "P",
                    "departmentId",
                    account.departmentId())),
            account.token())
        .path("data");
  }

  private Student student(Account account) {
    return students.findById(account.studentId()).orElseThrow();
  }

  private void patch(Account account, String json) throws Exception {
    ok("PATCH", TRANSFER_PATH, json, account.token());
  }

  private JsonNode progress(Account account) throws Exception {
    return ok("GET", "/api/graduation/progress", null, account.token()).path("data");
  }

  private JsonNode ok(String method, String path, String json, String token) throws Exception {
    var response = request(method, path, json, token);
    assertThat(response.statusCode()).as("%s %s: %s", method, path, response.body()).isEqualTo(200);
    return mapper.readTree(response.body());
  }

  private HttpResponse<String> request(String method, String path, String json, String token)
      throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return http.send(
        builder
            .method(
                method,
                json == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private record Account(UUID userId, UUID studentId, Long departmentId, String token) {}
}
