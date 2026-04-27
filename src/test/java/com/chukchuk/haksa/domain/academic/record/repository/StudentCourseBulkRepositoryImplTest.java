package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.domain.student.model.GradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StudentCourseBulkRepositoryImplTest.TestConfig.class)
class StudentCourseBulkRepositoryImplTest {

    @Configuration
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        StudentCourseBulkRepository studentCourseBulkRepository(JdbcTemplate jdbcTemplate) {
            return new StudentCourseBulkRepositoryImpl(jdbcTemplate);
        }
    }

    @Autowired
    private StudentCourseBulkRepository studentCourseBulkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS student_courses");
        jdbcTemplate.execute("""
                CREATE TABLE student_courses (
                    student_id UUID NOT NULL,
                    offering_id BIGINT NOT NULL,
                    grade VARCHAR(255),
                    points INTEGER,
                    is_retake BOOLEAN,
                    original_score INTEGER,
                    is_retake_deleted BOOLEAN,
                    created_at TIMESTAMP WITH TIME ZONE
                )
                """);
    }

    @Test
    void insertAll_insertsAllRowsInSingleBatch() {
        UUID studentId = UUID.randomUUID();
        StudentCourseBulkRow row = new StudentCourseBulkRow(
                studentId,
                10L,
                GradeType.A0,
                3,
                false,
                95,
                false
        );

        studentCourseBulkRepository.insertAll(List.of(row));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_courses WHERE student_id = ?",
                Integer.class,
                studentId
        );
        assertThat(count).isEqualTo(1);

        String storedGrade = jdbcTemplate.queryForObject(
                "SELECT grade FROM student_courses WHERE student_id = ?",
                String.class,
                studentId
        );
        assertThat(storedGrade).isEqualTo(GradeType.A0.getValue());
    }
}
