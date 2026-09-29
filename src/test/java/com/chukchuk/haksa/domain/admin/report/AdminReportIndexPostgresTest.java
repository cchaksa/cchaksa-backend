package com.chukchuk.haksa.domain.admin.report;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AdminReportIndexPostgresTest {
  private static EmbeddedPostgres postgres;
  private static DataSource dataSource;

  @BeforeAll
  static void setUp() throws Exception {
    postgres = EmbeddedPostgres.start();
    dataSource = postgres.getPostgresDatabase();
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE admin_accounts (id UUID PRIMARY KEY)");
      statement.execute(
          """
          CREATE TABLE reports (
              id UUID PRIMARY KEY,
              status VARCHAR(20) NOT NULL,
              student_code VARCHAR(255),
              created_at TIMESTAMP WITH TIME ZONE NOT NULL
          )
          """);
      ClassPathResource migration =
          new ClassPathResource("db/migration/V17__add_admin_report_management.sql");
      String migrationSql =
          new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      statement.execute(migrationSql);
      statement.execute(
          """
          INSERT INTO reports (id, status, student_code, created_at)
          SELECT md5(value::text)::uuid,
                 CASE WHEN value % 2 = 0 THEN 'PENDING' ELSE 'ANSWERED' END,
                 CASE WHEN value % 10 = 0 THEN '20201234' ELSE value::text END,
                 NOW() - value * INTERVAL '1 second'
          FROM generate_series(1, 5000) AS value
          """);
      statement.execute("ANALYZE reports");
    }
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (postgres != null) {
      postgres.close();
    }
  }

  @Test
  void latestStatusAndStudentQueriesUsePurposeBuiltIndexes() throws Exception {
    assertThat(
            explain(
                "SELECT id FROM reports "
                    + "WHERE status = 'PENDING' ORDER BY created_at DESC, id DESC LIMIT 20"))
        .contains("idx_reports_admin_status_created_id_desc");
    assertThat(
            explain(
                "SELECT id FROM reports "
                    + "WHERE student_code = '20201234' "
                    + "ORDER BY created_at DESC, id DESC LIMIT 20"))
        .contains("idx_reports_admin_student_code_created_id_desc");
    assertThat(explain("SELECT id FROM reports ORDER BY created_at DESC, id DESC LIMIT 20"))
        .contains("idx_reports_admin_created_id_desc");
  }

  private String explain(String query) throws Exception {
    StringBuilder plan = new StringBuilder();
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("SET enable_seqscan = off");
      try (ResultSet resultSet = statement.executeQuery("EXPLAIN " + query)) {
        while (resultSet.next()) {
          plan.append(resultSet.getString(1)).append('\n');
        }
      }
    }
    return plan.toString();
  }
}
