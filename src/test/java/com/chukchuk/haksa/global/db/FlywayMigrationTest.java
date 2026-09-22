package com.chukchuk.haksa.global.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class FlywayMigrationTest {

  @Test
  void freshDatabaseMigratesFromV1ToV15() throws Exception {
    String dbName = "flyway-migration-" + UUID.randomUUID();
    String url =
        "jdbc:h2:mem:"
            + dbName
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
            + "DB_CLOSE_DELAY=-1;"
            + "INIT=CREATE SCHEMA IF NOT EXISTS public";

    Flyway baselineFlyway =
        Flyway.configure()
            .dataSource(url, "sa", "")
            .schemas("public")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("1"))
            .load();

    baselineFlyway.migrate();

    try (var connection = DriverManager.getConnection(url, "sa", "")) {
      assertThat(hasColumn(connection, "raw_faculty_division_name")).isFalse();
      assertThat(hasColumn(connection, "scrape_jobs", "link_started_at")).isFalse();
      assertThat(hasColumn(connection, "scrape_jobs", "link_ended_at")).isFalse();
      assertThat(hasTable(connection, "language_cert_policy_groups")).isFalse();
    }

    Flyway flyway =
        Flyway.configure()
            .dataSource(url, "sa", "")
            .schemas("public")
            .locations("classpath:db/migration")
            .load();

    flyway.migrate();

    assertThat(flyway.info().applied())
        .extracting(info -> info.getVersion())
        .containsExactly(
            MigrationVersion.fromVersion("1"),
            MigrationVersion.fromVersion("2"),
            MigrationVersion.fromVersion("3"),
            MigrationVersion.fromVersion("4"),
            MigrationVersion.fromVersion("5"),
            MigrationVersion.fromVersion("6"),
            MigrationVersion.fromVersion("7"),
            MigrationVersion.fromVersion("8"),
            MigrationVersion.fromVersion("9"),
            MigrationVersion.fromVersion("10"),
            MigrationVersion.fromVersion("11"),
            MigrationVersion.fromVersion("12"),
            MigrationVersion.fromVersion("13"),
            MigrationVersion.fromVersion("14"),
            MigrationVersion.fromVersion("15"));

    try (var connection = DriverManager.getConnection(url, "sa", "")) {
      assertThat(hasColumn(connection, "raw_faculty_division_name")).isTrue();
      assertThat(rawFacultyDivisionNameColumnSize(connection)).isEqualTo(64);
      assertThat(hasTable(connection, "language_cert_policy_groups")).isTrue();
      assertThat(hasTable(connection, "language_cert_requirements")).isTrue();
      assertThat(hasTable(connection, "department_language_cert_policy_mappings")).isTrue();
      assertThat(hasColumn(connection, "scrape_jobs", "link_started_at")).isTrue();
      assertThat(hasColumn(connection, "scrape_jobs", "link_ended_at")).isTrue();
      assertThat(hasColumn(connection, "semester_academic_records", "lecture_evaluation_required"))
          .isFalse();
      assertThat(hasColumn(connection, "semester_academic_records", "lecture_evaluation_completed"))
          .isFalse();
      assertThat(hasColumn(connection, "semester_academic_records", "lecture_evaluation_status"))
          .isTrue();
      assertThat(hasTable(connection, "course_evaluations")).isTrue();
      assertThat(hasTable(connection, "course_evaluation_tags")).isTrue();
      assertThat(hasColumn(connection, "refresh_token", "session_id")).isTrue();
      assertThat(hasColumn(connection, "refresh_token", "token_hash")).isTrue();
      assertThat(isNullable(connection, "refresh_token", "token")).isTrue();
      assertThat(primaryKeyColumn(connection, "refresh_token")).isEqualTo("session_id");
      assertThat(isNullable(connection, "users", "email")).isTrue();
      assertThat(isNullable(connection, "social_accounts", "email")).isTrue();
      assertThat(hasTable(connection, "student_designated_courses")).isTrue();
      assertThat(hasColumn(connection, "students", "designated_courses_snapshot_version")).isTrue();
      assertThat(isNullable(connection, "students", "designated_courses_snapshot_version"))
          .isTrue();
      assertThat(hasColumn(connection, "students", "designated_courses_reset_at")).isTrue();
      assertThat(isNullable(connection, "students", "designated_courses_reset_at")).isTrue();
      assertThat(hasColumn(connection, "students", "transfer_registered_semesters")).isTrue();
      assertThat(isNullable(connection, "students", "transfer_registered_semesters")).isTrue();
      assertThat(
              hasColumn(connection, "student_graduation_progress", "graduation_review_fulfilled"))
          .isTrue();
      assertThat(hasTable(connection, "reports")).isTrue();
      assertThat(hasColumn(connection, "reports", "submitted_user_id")).isTrue();
      assertThat(hasColumn(connection, "reports", "graduation_requirement_status")).isTrue();
      assertThat(hasIndex(connection, "reports", "idx_reports_user_created_id_desc")).isTrue();
      assertThat(indexColumns(connection, "idx_reports_user_created_id_desc"))
          .containsExactly("user_id:ASC", "created_at:DESC", "id:DESC");
      assertThat((int) foreignKeyDeleteRule(connection, "reports", "fk_reports_user_id"))
          .isIn(
              java.sql.DatabaseMetaData.importedKeyNoAction,
              java.sql.DatabaseMetaData.importedKeyRestrict);
      assertThat(
              isNullable(connection, "student_graduation_progress", "graduation_review_fulfilled"))
          .isTrue();
      assertThat(
              (int)
                  foreignKeyDeleteRule(
                      connection,
                      "student_designated_courses",
                      "fk_student_designated_courses_student_id"))
          .isEqualTo(java.sql.DatabaseMetaData.importedKeyCascade);
      try (var statement = connection.createStatement();
          var resultSet =
              statement.executeQuery(
                  "SELECT area_name, is_active FROM public.liberal_arts_area_codes "
                      + "WHERE code = 8")) {
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString("area_name")).isEqualTo("8영역");
        assertThat(resultSet.getBoolean("is_active")).isTrue();
      }
    }
  }

  @Test
  void v15UpgradesV14AndEnforcesReportStateConstraints() throws Exception {
    String dbName = "flyway-v15-reports-" + UUID.randomUUID();
    String url =
        "jdbc:h2:mem:"
            + dbName
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
            + "DB_CLOSE_DELAY=-1;"
            + "INIT=CREATE SCHEMA IF NOT EXISTS public";

    Flyway.configure()
        .dataSource(url, "sa", "")
        .schemas("public")
        .locations("classpath:db/migration")
        .target(MigrationVersion.fromVersion("14"))
        .load()
        .migrate();

    try (var connection = DriverManager.getConnection(url, "sa", "")) {
      assertThat(hasTable(connection, "reports")).isFalse();
    }

    Flyway.configure()
        .dataSource(url, "sa", "")
        .schemas("public")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    UUID userId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO public.users (id, is_deleted) VALUES ('%s', FALSE)".formatted(userId));
      assertReportInsertRejected(
          statement, reportId, userId, "ANSWERED", "NULL", "NULL", "AVAILABLE");
      assertReportInsertRejected(
          statement,
          UUID.randomUUID(),
          userId,
          "PENDING",
          "'답변'",
          "CURRENT_TIMESTAMP",
          "AVAILABLE");
      assertReportInsertRejected(
          statement,
          UUID.randomUUID(),
          userId,
          "ANSWERED",
          "'   '",
          "CURRENT_TIMESTAMP",
          "AVAILABLE");
      assertReportInsertRejected(
          statement, UUID.randomUUID(), userId, "INVALID", "NULL", "NULL", "AVAILABLE");
      assertReportInsertRejected(
          statement, UUID.randomUUID(), userId, "PENDING", "NULL", "NULL", "INVALID");
    }
  }

  @Test
  void v11BackfillsTransferStudentFlagFromAdmissionType() throws Exception {
    String dbName = "flyway-v11-transfer-" + UUID.randomUUID();
    String url =
        "jdbc:h2:mem:"
            + dbName
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
            + "DB_CLOSE_DELAY=-1;"
            + "INIT=CREATE SCHEMA IF NOT EXISTS public";

    Flyway.configure()
        .dataSource(url, "sa", "")
        .schemas("public")
        .locations("classpath:db/migration")
        .target(MigrationVersion.fromVersion("10"))
        .load()
        .migrate();

    UUID transferUserId = UUID.randomUUID();
    UUID legacyTransferUserId = UUID.randomUUID();
    UUID generalUserId = UUID.randomUUID();
    UUID nullAdmissionUserId = UUID.randomUUID();
    UUID transferStudentId = UUID.randomUUID();
    UUID legacyTransferStudentId = UUID.randomUUID();
    UUID generalStudentId = UUID.randomUUID();
    UUID nullAdmissionStudentId = UUID.randomUUID();

    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          INSERT INTO public.departments (id, department_code, established_department_name)
          VALUES (1001, 'MIGRATION-TRANSFER', '마이그레이션 편입 테스트학과')
          """);
      statement.executeUpdate(
          """
          INSERT INTO public.users (id, email, is_deleted)
          VALUES
              ('%s', 'transfer@example.com', FALSE),
              ('%s', 'legacy-transfer@example.com', FALSE),
              ('%s', 'general@example.com', FALSE),
              ('%s', 'null-admission@example.com', FALSE)
          """
              .formatted(transferUserId, legacyTransferUserId, generalUserId, nullAdmissionUserId));
      statement.executeUpdate(
          """
          INSERT INTO public.students (
              student_id, student_code, admission_type, reconnection_required,
              admission_year, department_id, user_id
          )
          VALUES
              ('%s', 'TRANSFER-2', '2', FALSE, 2022, 1001, '%s'),
              ('%s', 'TRANSFER-HANGUL', ' 일반편입 ', FALSE, 2022, 1001, '%s'),
              ('%s', 'GENERAL', '신입', FALSE, 2022, 1001, '%s'),
              ('%s', 'NULL-ADMISSION', NULL, FALSE, 2022, 1001, '%s')
          """
              .formatted(
                  transferStudentId,
                  transferUserId,
                  legacyTransferStudentId,
                  legacyTransferUserId,
                  generalStudentId,
                  generalUserId,
                  nullAdmissionStudentId,
                  nullAdmissionUserId));
    }

    Flyway.configure()
        .dataSource(url, "sa", "")
        .schemas("public")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement();
        var resultSet =
            statement.executeQuery(
                """
                SELECT student_code, is_transfer_student
                FROM public.students
                ORDER BY student_code
                """)) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getString("student_code")).isEqualTo("GENERAL");
      assertThat(resultSet.getBoolean("is_transfer_student")).isFalse();
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getString("student_code")).isEqualTo("NULL-ADMISSION");
      assertThat(resultSet.getObject("is_transfer_student")).isNull();
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getString("student_code")).isEqualTo("TRANSFER-2");
      assertThat(resultSet.getBoolean("is_transfer_student")).isTrue();
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getString("student_code")).isEqualTo("TRANSFER-HANGUL");
      assertThat(resultSet.getBoolean("is_transfer_student")).isTrue();
    }
  }

  @Test
  void refreshTokenMigrationHandlesDefaultPrimaryKeyConstraintName() throws Exception {
    String dbName = "flyway-migration-" + UUID.randomUUID();
    String url =
        "jdbc:h2:mem:"
            + dbName
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
            + "DB_CLOSE_DELAY=-1;"
            + "INIT=CREATE SCHEMA IF NOT EXISTS public";

    Flyway flywayToV7 =
        Flyway.configure()
            .dataSource(url, "sa", "")
            .schemas("public")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("7"))
            .load();

    flywayToV7.migrate();

    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement()) {
      statement.execute("ALTER TABLE public.refresh_token DROP CONSTRAINT pk_refresh_token");
      statement.execute(
          "ALTER TABLE public.refresh_token ADD CONSTRAINT refresh_token_pkey "
              + "PRIMARY KEY (user_id)");
      statement.execute(
          """
                    INSERT INTO public.refresh_token (user_id, token, expiry)
                    VALUES ('user-a', 'refresh-token-a', CURRENT_TIMESTAMP)
          """);
    }

    Flyway flyway =
        Flyway.configure()
            .dataSource(url, "sa", "")
            .schemas("public")
            .locations("classpath:db/migration")
            .load();

    assertThatCode(flyway::migrate).doesNotThrowAnyException();

    try (var connection = DriverManager.getConnection(url, "sa", "")) {
      assertThat(primaryKeyColumn(connection, "refresh_token")).isEqualTo("session_id");
    }
  }

  @Test
  void v7InitializesNullLectureEvaluationStatusFromSemesterCourses() throws Exception {
    String dbName = "flyway-v7-status-" + UUID.randomUUID();
    String url =
        "jdbc:h2:mem:"
            + dbName
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
            + "DB_CLOSE_DELAY=-1;"
            + "INIT=CREATE SCHEMA IF NOT EXISTS public";

    Flyway.configure()
        .dataSource(url, "sa", "")
        .schemas("public")
        .locations("classpath:db/migration")
        .target(MigrationVersion.fromVersion("6"))
        .load()
        .migrate();

    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();

    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement()) {
      statement.executeUpdate(
          """
                    INSERT INTO public.users (id, email, is_deleted)
                    VALUES ('%s', 'migration-test@example.com', FALSE)
          """
              .formatted(userId));
      statement.executeUpdate(
          """
                    INSERT INTO public.departments (id, department_code, established_department_name)
                    VALUES (1001, 'MIGRATION-TEST', '마이그레이션 테스트학과')
          """);
      statement.executeUpdate(
          """
                    INSERT INTO public.students (
                        student_id, student_code, reconnection_required, admission_year, department_id, user_id
                    )
                    VALUES ('%s', 'MIGRATION-TEST-STUDENT', FALSE, 2020, 1001, '%s')
          """
              .formatted(studentId, userId));
      statement.executeUpdate(
          """
                    INSERT INTO public.semester_academic_records (id, semester, year, student_id)
                    VALUES
                        ('%s', 10, 2026, '%s'),
                        ('%s', 20, 2026, '%s')
          """
              .formatted(UUID.randomUUID(), studentId, UUID.randomUUID(), studentId));
      statement.executeUpdate(
          """
                    INSERT INTO public.courses (id, course_code, course_name)
                    VALUES
                        (2001, 'MIG-IP', 'IP 과목'),
                        (2002, 'MIG-A', '완료 과목')
          """);
      statement.executeUpdate(
          """
                    INSERT INTO public.course_offerings (id, year, semester, points, course_id)
                    VALUES
                        (3001, 2026, 10, 3, 2001),
                        (3002, 2026, 20, 3, 2002)
          """);
      statement.executeUpdate(
          """
                    INSERT INTO public.student_courses (
                        grade, points, is_retake, original_score, is_retake_deleted, offering_id, student_id
                    )
                    VALUES
                        ('IP', 3, FALSE, NULL, FALSE, 3001, '%s'),
                        ('A+', 3, FALSE, 95, FALSE, 3002, '%s')
          """
              .formatted(studentId, studentId));
    }

    Flyway.configure()
        .dataSource(url, "sa", "")
        .schemas("public")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement()) {
      try (var resultSet =
          statement.executeQuery(
              """
                    SELECT semester, lecture_evaluation_status
                    FROM public.semester_academic_records
                    ORDER BY semester
              """)) {
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getInt("semester")).isEqualTo(10);
        assertThat(resultSet.getString("lecture_evaluation_status")).isEqualTo("NOT_RELEASED");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getInt("semester")).isEqualTo(20);
        assertThat(resultSet.getString("lecture_evaluation_status")).isEqualTo("PENDING");
      }
    }
  }

  @Test
  void v7SkipsLectureEvaluationStatusUpdateWhenSemesterHasNoCourses() throws Exception {
    String migrationSql =
        Files.readString(
            Path.of(
                "src/main/resources/db/migration/"
                    + "V7__add_not_released_lecture_evaluation_status.sql"));

    assertThat(migrationSql).contains("WHERE sar.lecture_evaluation_status IS NULL\n  AND EXISTS");
    assertThat(migrationSql).doesNotContain("ELSE NULL");
  }

  private boolean hasColumn(Connection connection, String columnName) throws Exception {
    return hasColumn(connection, "course_offerings", columnName);
  }

  private boolean hasColumn(Connection connection, String tableName, String columnName)
      throws Exception {
    try (var columns = connection.getMetaData().getColumns(null, "public", tableName, columnName)) {
      return columns.next();
    }
  }

  private int rawFacultyDivisionNameColumnSize(Connection connection) throws Exception {
    try (var columns =
        connection
            .getMetaData()
            .getColumns(null, "public", "course_offerings", "raw_faculty_division_name")) {
      assertThat(columns.next()).isTrue();
      return columns.getInt("COLUMN_SIZE");
    }
  }

  private boolean hasTable(Connection connection, String tableName) throws Exception {
    try (var tables =
        connection.getMetaData().getTables(null, "public", tableName, new String[] {"TABLE"})) {
      return tables.next();
    }
  }

  private boolean hasIndex(Connection connection, String tableName, String indexName)
      throws Exception {
    try (var indexes =
        connection.getMetaData().getIndexInfo(null, "public", tableName, false, false)) {
      while (indexes.next()) {
        if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
          return true;
        }
      }
      return false;
    }
  }

  private List<String> indexColumns(Connection connection, String indexName) throws Exception {
    List<String> columns = new ArrayList<>();
    try (var statement =
        connection.prepareStatement(
            """
            SELECT COLUMN_NAME, ORDERING_SPECIFICATION
            FROM INFORMATION_SCHEMA.INDEX_COLUMNS
            WHERE TABLE_SCHEMA = 'public' AND INDEX_NAME = ?
            ORDER BY ORDINAL_POSITION
            """)) {
      statement.setString(1, indexName);
      try (var resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          columns.add(
              resultSet.getString("column_name")
                  + ":"
                  + resultSet.getString("ordering_specification"));
        }
      }
    }
    return columns;
  }

  private boolean isNullable(Connection connection, String tableName, String columnName)
      throws Exception {
    try (var columns = connection.getMetaData().getColumns(null, "public", tableName, columnName)) {
      assertThat(columns.next()).isTrue();
      return columns.getInt("NULLABLE") == java.sql.DatabaseMetaData.columnNullable;
    }
  }

  private String primaryKeyColumn(Connection connection, String tableName) throws Exception {
    try (var primaryKeys = connection.getMetaData().getPrimaryKeys(null, "public", tableName)) {
      assertThat(primaryKeys.next()).isTrue();
      return primaryKeys.getString("COLUMN_NAME");
    }
  }

  private short foreignKeyDeleteRule(Connection connection, String tableName, String fkName)
      throws Exception {
    try (var foreignKeys = connection.getMetaData().getImportedKeys(null, "public", tableName)) {
      while (foreignKeys.next()) {
        if (fkName.equalsIgnoreCase(foreignKeys.getString("FK_NAME"))) {
          return foreignKeys.getShort("DELETE_RULE");
        }
      }
    }
    throw new AssertionError("외래 키를 찾을 수 없습니다: " + fkName);
  }

  private void assertReportInsertRejected(
      Statement statement,
      UUID reportId,
      UUID userId,
      String status,
      String answerSql,
      String answeredAtSql,
      String graduationStatus) {
    assertThatThrownBy(
            () ->
                statement.executeUpdate(
                    """
                    INSERT INTO public.reports (
                        id, user_id, title, content, status, answer, answered_at,
                        graduation_requirement_status, created_at, updated_at
                    ) VALUES (
                        '%s', '%s', '제목', '본문', '%s', %s, %s,
                        '%s', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """
                        .formatted(
                            reportId, userId, status, answerSql, answeredAtSql, graduationStatus)))
        .isInstanceOf(java.sql.SQLException.class);
  }
}
