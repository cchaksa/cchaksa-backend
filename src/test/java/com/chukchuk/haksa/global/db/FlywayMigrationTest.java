package com.chukchuk.haksa.global.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    @Test
    void freshDatabaseMigratesFromV1ToV7() throws Exception {
        String dbName = "flyway-migration-" + UUID.randomUUID();
        String url = "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
                + "DB_CLOSE_DELAY=-1;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS public";

        Flyway baselineFlyway = Flyway.configure()
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

        Flyway flyway = Flyway.configure()
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
                        MigrationVersion.fromVersion("7")
                );

        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(hasColumn(connection, "raw_faculty_division_name")).isTrue();
            assertThat(rawFacultyDivisionNameColumnSize(connection)).isEqualTo(64);
            assertThat(hasTable(connection, "language_cert_policy_groups")).isTrue();
            assertThat(hasTable(connection, "language_cert_requirements")).isTrue();
            assertThat(hasTable(connection, "department_language_cert_policy_mappings")).isTrue();
            assertThat(hasColumn(connection, "scrape_jobs", "link_started_at")).isTrue();
            assertThat(hasColumn(connection, "scrape_jobs", "link_ended_at")).isTrue();
            assertThat(hasColumn(connection, "semester_academic_records", "lecture_evaluation_required")).isFalse();
            assertThat(hasColumn(connection, "semester_academic_records", "lecture_evaluation_completed")).isFalse();
            assertThat(hasColumn(connection, "semester_academic_records", "lecture_evaluation_status")).isTrue();
            assertThat(hasTable(connection, "course_evaluations")).isTrue();
            assertThat(hasTable(connection, "course_evaluation_tags")).isTrue();
        }
    }

    @Test
    void v7InitializesNullLectureEvaluationStatusFromSemesterCourses() throws Exception {
        String dbName = "flyway-v7-status-" + UUID.randomUUID();
        String url = "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
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
            statement.executeUpdate("""
                    INSERT INTO public.users (id, email, is_deleted)
                    VALUES ('%s', 'migration-test@example.com', FALSE)
                    """.formatted(userId));
            statement.executeUpdate("""
                    INSERT INTO public.departments (id, department_code, established_department_name)
                    VALUES (1001, 'MIGRATION-TEST', '마이그레이션 테스트학과')
                    """);
            statement.executeUpdate("""
                    INSERT INTO public.students (
                        student_id, student_code, reconnection_required, admission_year, department_id, user_id
                    )
                    VALUES ('%s', 'MIGRATION-TEST-STUDENT', FALSE, 2020, 1001, '%s')
                    """.formatted(studentId, userId));
            statement.executeUpdate("""
                    INSERT INTO public.semester_academic_records (id, semester, year, student_id)
                    VALUES
                        ('%s', 10, 2026, '%s'),
                        ('%s', 20, 2026, '%s')
                    """.formatted(UUID.randomUUID(), studentId, UUID.randomUUID(), studentId));
            statement.executeUpdate("""
                    INSERT INTO public.courses (id, course_code, course_name)
                    VALUES
                        (2001, 'MIG-IP', 'IP 과목'),
                        (2002, 'MIG-A', '완료 과목')
                    """);
            statement.executeUpdate("""
                    INSERT INTO public.course_offerings (id, year, semester, points, course_id)
                    VALUES
                        (3001, 2026, 10, 3, 2001),
                        (3002, 2026, 20, 3, 2002)
                    """);
            statement.executeUpdate("""
                    INSERT INTO public.student_courses (
                        grade, points, is_retake, original_score, is_retake_deleted, offering_id, student_id
                    )
                    VALUES
                        ('IP', 3, FALSE, NULL, FALSE, 3001, '%s'),
                        ('A+', 3, FALSE, 95, FALSE, 3002, '%s')
                    """.formatted(studentId, studentId));
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .schemas("public")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery("""
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
        String migrationSql = Files.readString(Path.of(
                "src/main/resources/db/migration/V7__add_not_released_lecture_evaluation_status.sql"
        ));

        assertThat(migrationSql)
                .contains("WHERE sar.lecture_evaluation_status IS NULL\n  AND EXISTS");
        assertThat(migrationSql)
                .doesNotContain("ELSE NULL");
    }

    private boolean hasColumn(Connection connection, String columnName) throws Exception {
        return hasColumn(connection, "course_offerings", columnName);
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", tableName, columnName)) {
            return columns.next();
        }
    }

    private int rawFacultyDivisionNameColumnSize(Connection connection) throws Exception {
        try (var columns = connection.getMetaData().getColumns(
                null,
                "public",
                "course_offerings",
                "raw_faculty_division_name"
        )) {
            assertThat(columns.next()).isTrue();
            return columns.getInt("COLUMN_SIZE");
        }
    }

    private boolean hasTable(Connection connection, String tableName) throws Exception {
        try (var tables = connection.getMetaData().getTables(null, "public", tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
