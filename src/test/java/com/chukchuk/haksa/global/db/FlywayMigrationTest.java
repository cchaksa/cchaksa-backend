package com.chukchuk.haksa.global.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FlywayMigrationTest {

    @Test
    void freshDatabaseMigratesFromV1ToV5() throws Exception {
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
                        MigrationVersion.fromVersion("5")
                );

        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(hasColumn(connection, "raw_faculty_division_name")).isTrue();
            assertThat(rawFacultyDivisionNameColumnSize(connection)).isEqualTo(64);
            assertThat(hasTable(connection, "language_cert_policy_groups")).isTrue();
            assertThat(hasTable(connection, "language_cert_requirements")).isTrue();
            assertThat(hasTable(connection, "department_language_cert_policy_mappings")).isTrue();
            assertThat(hasColumn(connection, "scrape_jobs", "link_started_at")).isTrue();
            assertThat(hasColumn(connection, "scrape_jobs", "link_ended_at")).isTrue();
            assertThat(hasColumn(connection, "refresh_token", "session_id")).isTrue();
            assertThat(hasColumn(connection, "refresh_token", "token_hash")).isTrue();
            assertThat(isNullable(connection, "refresh_token", "token")).isTrue();
            assertThat(primaryKeyColumn(connection, "refresh_token")).isEqualTo("session_id");
        }
    }

    @Test
    void refreshTokenMigrationHandlesDefaultPrimaryKeyConstraintName() throws Exception {
        String dbName = "flyway-migration-" + UUID.randomUUID();
        String url = "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=YEAR;"
                + "DB_CLOSE_DELAY=-1;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS public";

        Flyway flywayToV4 = Flyway.configure()
                .dataSource(url, "sa", "")
                .schemas("public")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("4"))
                .load();

        flywayToV4.migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE public.refresh_token DROP CONSTRAINT pk_refresh_token");
            statement.execute("ALTER TABLE public.refresh_token ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (user_id)");
            statement.execute("""
                    INSERT INTO public.refresh_token (user_id, token, expiry)
                    VALUES ('user-a', 'refresh-token-a', CURRENT_TIMESTAMP)
                    """);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .schemas("public")
                .locations("classpath:db/migration")
                .load();

        assertThatCode(flyway::migrate)
                .doesNotThrowAnyException();

        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(primaryKeyColumn(connection, "refresh_token")).isEqualTo("session_id");
        }
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

    private boolean isNullable(Connection connection, String tableName, String columnName) throws Exception {
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
}
