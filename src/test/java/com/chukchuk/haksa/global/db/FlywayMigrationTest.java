package com.chukchuk.haksa.global.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    @Test
    void freshDatabaseMigratesFromV1ToV2() throws Exception {
        String url = "jdbc:h2:mem:flyway-migration;MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
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
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .schemas("public")
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        assertThat(flyway.info().applied())
                .extracting(info -> info.getVersion())
                .containsExactly(MigrationVersion.fromVersion("1"), MigrationVersion.fromVersion("2"));

        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(hasColumn(connection, "raw_faculty_division_name")).isTrue();
            assertThat(rawFacultyDivisionNameColumnSize(connection)).isEqualTo(64);
        }
    }

    private boolean hasColumn(Connection connection, String columnName) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", "course_offerings", columnName)) {
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
}
