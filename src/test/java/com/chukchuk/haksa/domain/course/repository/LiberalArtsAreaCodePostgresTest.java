// 실제 PostgreSQL에서 교양 영역 자동 등록의 동시성과 FK 저장을 검증한다.
package com.chukchuk.haksa.domain.course.repository;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class LiberalArtsAreaCodePostgresTest {

    private static EmbeddedPostgres postgres;
    private static DataSource dataSource;

    @BeforeAll
    static void setUp() throws Exception {
        postgres = EmbeddedPostgres.start();
        dataSource = postgres.getPostgresDatabase();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE liberal_arts_area_codes (
                        code INTEGER PRIMARY KEY,
                        area_name VARCHAR(255) NOT NULL,
                        is_active BOOLEAN NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE course_offerings (
                        id BIGSERIAL PRIMARY KEY,
                        area_code INTEGER REFERENCES liberal_arts_area_codes(code)
                    )
                    """);
        }
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    @Test
    void concurrentRegistrationAllowsOfferingFkInSameTransaction() throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> registerAndStoreOffering(start, 8));
            Future<?> second = executor.submit(() -> registerAndStoreOffering(start, 8));

            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        assertThat(countRows("liberal_arts_area_codes")).isEqualTo(1);
        assertThat(countRows("course_offerings")).isEqualTo(2);
    }

    private static void registerAndStoreOffering(CyclicBarrier start, int code) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            start.await();
            try (PreparedStatement registration = connection.prepareStatement(registrationSql())) {
                registration.setInt(1, code);
                registration.setString(2, code + "영역");
                registration.executeUpdate();
            }
            try (PreparedStatement offering = connection.prepareStatement(
                    "INSERT INTO course_offerings (area_code) VALUES (?)")) {
                offering.setInt(1, code);
                offering.executeUpdate();
            }
            connection.commit();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String registrationSql() throws NoSuchMethodException {
        Query query = LiberalArtsAreaCodeRepository.class
                .getMethod("insertIfAbsent", Integer.class, String.class)
                .getAnnotation(Query.class);
        return query.value().replace(":code", "?").replace(":areaName", "?");
    }

    private static int countRows(String table) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }
}
