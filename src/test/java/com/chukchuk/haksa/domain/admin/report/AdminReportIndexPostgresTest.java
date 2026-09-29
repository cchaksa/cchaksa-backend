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
              user_id UUID NOT NULL,
              status VARCHAR(20) NOT NULL,
              student_code VARCHAR(255),
              created_at TIMESTAMP WITH TIME ZONE NOT NULL
          )
          """);
      statement.execute(
          "CREATE INDEX idx_reports_user_created_id_desc "
              + "ON reports (user_id, created_at DESC, id DESC)");
      ClassPathResource migration =
          new ClassPathResource("db/migration/V17__add_admin_report_management.sql");
      String migrationSql =
          new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      statement.execute(migrationSql);
      statement.execute(
          """
          INSERT INTO reports (id, user_id, status, student_code, created_at)
          SELECT md5(value::text)::uuid,
                 md5((value % 4000)::text)::uuid,
                 CASE WHEN value % 10 = 0 THEN 'PENDING' ELSE 'ANSWERED' END,
                 LPAD((value % 4000)::text, 8, '0'),
                 NOW() - value * INTERVAL '1 second'
          FROM generate_series(1, 20000) AS value
          """);
      statement.execute("VACUUM ANALYZE reports");
    }
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (postgres != null) {
      postgres.close();
    }
  }

  @Test
  void latestAndExactQueriesUseExpectedBtreeIndexes() throws Exception {
    assertIndexedWithoutSort(
        "SELECT id FROM reports ORDER BY created_at DESC, id DESC LIMIT 20",
        "idx_reports_admin_created_id_desc");
    assertIndexedWithoutSort(
        "SELECT id FROM reports "
            + "WHERE status = 'PENDING' ORDER BY created_at DESC, id DESC LIMIT 20",
        "idx_reports_admin_status_created_id_desc");
    assertIndexed(
        "SELECT id FROM reports "
            + "WHERE user_id = md5('1234')::uuid AND status = 'PENDING' "
            + "ORDER BY created_at DESC, id DESC LIMIT 20",
        "idx_reports_user_created_id_desc");
    assertIndexed(
        "SELECT id FROM reports "
            + "WHERE student_code = '00001230' AND status = 'PENDING' "
            + "ORDER BY created_at DESC, id DESC LIMIT 20",
        "idx_reports_admin_student_code_created_id_desc");
  }

  @Test
  void exactCountQueriesUseBtreeIndexes() throws Exception {
    assertThat(explain("SELECT count(*) FROM reports WHERE user_id = md5('1234')::uuid"))
        .contains("idx_reports_user_created_id_desc", "Buffers:");
    assertThat(explain("SELECT count(*) FROM reports WHERE student_code = '00001234'"))
        .contains("idx_reports_admin_student_code_created_id_desc", "Buffers:");
  }

  @Test
  void largeOffsetComputesSkippedRowsWhileKeepingDeterministicIndexOrder() throws Exception {
    String plan =
        explain(
            "SELECT id FROM reports " + "ORDER BY created_at DESC, id DESC LIMIT 20 OFFSET 2000");

    assertThat(plan)
        .contains("idx_reports_admin_created_id_desc", "actual rows=2020", "Buffers:")
        .doesNotContain("Sort");
  }

  @Test
  void leadingWildcardHasNoSelectiveBtreeIndexCondition() throws Exception {
    String plan = explain("SELECT count(*) FROM reports WHERE student_code LIKE '%1234%'");

    assertThat(plan).contains("Seq Scan on reports", "Filter:", "Buffers:");
    assertThat(plan).doesNotContain("Index Cond:");
  }

  private String explain(String query) throws Exception {
    StringBuilder plan = new StringBuilder();
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      try (ResultSet resultSet =
          statement.executeQuery(
              "EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF, SUMMARY OFF) " + query)) {
        while (resultSet.next()) {
          plan.append(resultSet.getString(1)).append('\n');
        }
      }
    }
    return plan.toString();
  }

  private void assertIndexedWithoutSort(String query, String indexName) throws Exception {
    assertThat(explain(query)).contains(indexName, "Buffers:").doesNotContain("Sort");
  }

  private void assertIndexed(String query, String indexName) throws Exception {
    assertThat(explain(query)).contains(indexName, "Buffers:");
  }
}
