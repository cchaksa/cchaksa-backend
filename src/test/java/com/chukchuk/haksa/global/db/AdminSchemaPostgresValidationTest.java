package com.chukchuk.haksa.global.db;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true",
      "scraping.scheduler.enabled=false",
      "scraping.publisher.enabled=false",
      "scraping.stale.enabled=false"
    })
@ActiveProfiles("test")
class AdminSchemaPostgresValidationTest {
  private static final String TOKEN_HASH = "a".repeat(64);
  private static final EmbeddedPostgres POSTGRES = startPostgresAtV20();

  @Autowired private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
    registry.add("spring.datasource.username", () -> "postgres");
    registry.add("spring.datasource.password", () -> "");
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

  @AfterAll
  static void tearDown() throws Exception {
    POSTGRES.close();
  }

  @Test
  void v21DropsAdminSocialCredentialsAndHibernateValidatesPostgresSchema() {
    var column =
        jdbcTemplate.queryForMap(
            """
            SELECT data_type, character_maximum_length
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'admin_sessions'
              AND column_name = 'token_hash'
            """);

    assertThat(column.get("data_type")).isEqualTo("character varying");
    assertThat(column.get("character_maximum_length")).isEqualTo(64);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT token_hash FROM public.admin_sessions", String.class))
        .isEqualTo(TOKEN_HASH);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'admin_login_challenges'
                """,
                Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'admin_accounts'
                  AND column_name IN ('provider', 'social_id')
                """,
                Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conrelid = 'public.admin_accounts'::regclass
                  AND conname IN (
                      'uq_admin_accounts_provider_social_id',
                      'chk_admin_accounts_provider'
                  )
                """,
                Integer.class))
        .isZero();
  }

  private static EmbeddedPostgres startPostgresAtV20() {
    try {
      EmbeddedPostgres postgres = EmbeddedPostgres.start();
      String url = postgres.getJdbcUrl("postgres", "postgres");
      Flyway.configure()
          .dataSource(url, "postgres", "")
          .schemas("public")
          .locations("classpath:db/migration")
          .target(MigrationVersion.fromVersion("20"))
          .load()
          .migrate();

      UUID accountId = UUID.randomUUID();
      try (var connection = DriverManager.getConnection(url, "postgres", "");
          var statement = connection.createStatement()) {
        statement.executeUpdate(
            """
            INSERT INTO public.admin_accounts (
                id, login_id, password_hash, display_name, admin_role, status,
                created_by, created_at, updated_at
            ) VALUES (
                '%s', 'schema.validation', '$2a$12$testHash', '스키마 검증 관리자',
                'ADMIN', 'ACTIVE', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            """
                .formatted(accountId));
        statement.executeUpdate(
            """
            INSERT INTO public.admin_sessions (
                id, admin_account_id, token_hash, expires_at, idle_expires_at,
                last_accessed_at, created_at
            ) VALUES (
                '%s', '%s', '%s', CURRENT_TIMESTAMP + INTERVAL '8 hours',
                CURRENT_TIMESTAMP + INTERVAL '30 minutes', CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """
                .formatted(UUID.randomUUID(), accountId, TOKEN_HASH));
      }
      return postgres;
    } catch (Exception exception) {
      throw new IllegalStateException("PostgreSQL V18 fixture를 준비할 수 없습니다.", exception);
    }
  }
}
