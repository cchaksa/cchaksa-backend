// 실행 중인 서버의 인증된 편입 진단 응답과 OpenAPI 계약을 검증한다.

package com.chukchuk.haksa.domain.graduation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:transfer-http-341;MODE=PostgreSQL;"
          + "DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR,SEMESTER",
      "scraping.scheduler.enabled=false",
      "scraping.publisher.enabled=false",
      "scraping.stale.enabled=false",
      "security.jwt.access-expiration=60000"
    })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransferGraduationProgressHttpIntegrationTest {
  @Autowired private TestRestTemplate http;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DepartmentRepository departments;
  @Autowired private UserRepository users;
  @Autowired private StudentRepository students;
  @Autowired private CourseRepository courses;
  @Autowired private CourseOfferingRepository offerings;
  @Autowired private StudentCourseRepository studentCourses;

  @Test
  void servesHalfCreditComparisonThroughAuthenticatedHttp() {
    Department department = departments.save(new Department("http-341", "편입 검증 학과"));
    User user =
        users.save(
            User.builder()
                .email("transfer-http@example.com")
                .profileNickname("transfer-http")
                .build());
    Student student =
        students.save(
            Student.builder()
                .studentCode("2026341001")
                .name("편입 검증 학생")
                .department(department)
                .admissionYear(2026)
                .isTransferStudent(true)
                .isGraduated(false)
                .status(StudentStatus.재학)
                .gradeLevel(3)
                .completedSemesters(1)
                .user(user)
                .build());
    saveRequirement(department, "전핵", 19);
    saveRequirement(department, "전선", 96);
    saveCourse(student, "CORE-HTTP", FacultyDivision.전핵, 9);
    saveCourse(student, "ELECTIVE-HTTP", FacultyDivision.전선, 48);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(
        jwtProvider.createAccessToken(user.getId().toString(), user.getEmail(), "USER"));

    var response =
        http.exchange(
            "/api/graduation/progress", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode data = response.getBody().path("data");
    assertThat(data.path("analysisType").asText()).isEqualTo("TRANSFER");
    assertThat(data.path("analysisStatus").asText()).isEqualTo("MANUAL_REVIEW_REQUIRED");
    JsonNode areas = data.path("transferProgress").path("areas");
    assertThat(areas.size()).isEqualTo(2);
    assertThat(areas.get(0).path("areaType").asText()).isEqualTo("전핵");
    assertThat(areas.get(0).path("evaluationType").asText()).isEqualTo("COMPARISON");
    assertThat(areas.get(0).path("requiredCredits").decimalValue()).isEqualByComparingTo("9.5");
    assertThat(areas.get(0).path("earnedCredits").asInt()).isEqualTo(9);
    assertThat(areas.get(0).path("fulfilled").asBoolean()).isFalse();
    assertThat(areas.get(0).path("requiredCourses").isEmpty()).isTrue();
    assertThat(areas.get(1).path("requiredCredits").asInt()).isEqualTo(48);
    assertThat(areas.get(1).path("fulfilled").asBoolean()).isTrue();

    var docs = http.getForEntity("/v3/api-docs", JsonNode.class);
    assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode properties =
        docs.getBody()
            .path("components")
            .path("schemas")
            .path("TransferAreaProgressDto")
            .path("properties");
    assertThat(properties.path("requiredCredits").path("type").asText()).isEqualTo("number");
    assertThat(properties.path("requiredCredits").path("description").asText()).contains("50%");
    assertThat(properties.path("requiredCourses").path("description").asText()).contains("빈 목록");
  }

  private void saveRequirement(Department department, String area, int credits) {
    jdbc.update(
        "INSERT INTO department_area_requirements "
            + "(id, department_id, admission_year, area_type, required_credits) "
            + "VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        department.getId(),
        2024,
        area,
        credits);
  }

  private void saveCourse(Student student, String code, FacultyDivision area, int credits) {
    Course course = courses.save(new Course(code, code));
    CourseOffering offering =
        offerings.save(
            new CourseOffering(
                null, false, 2026, 1, null, "A", null, null, credits, null, area, course, null,
                null, null));
    studentCourses.save(
        new StudentCourse(student, offering, new Grade(GradeType.P), credits, false, null, false));
  }
}
