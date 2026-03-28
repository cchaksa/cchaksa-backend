package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.domain.student.model.GradeType;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class StudentCourseBulkRepositoryImpl implements StudentCourseBulkRepository {

    private static final String INSERT_SQL = """
            INSERT INTO student_courses
            (student_id, offering_id, grade, points, is_retake, original_score, is_retake_deleted, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public StudentCourseBulkRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertAll(List<StudentCourseBulkRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        Instant createdAt = Instant.now();
        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                StudentCourseBulkRow row = rows.get(i);
                ps.setObject(1, row.studentId());
                ps.setLong(2, row.offeringId());
                GradeType gradeType = row.gradeType();
                if (gradeType != null) {
                    ps.setString(3, gradeType.getValue());
                } else {
                    ps.setNull(3, java.sql.Types.VARCHAR);
                }
                if (row.points() != null) {
                    ps.setInt(4, row.points());
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }
                ps.setBoolean(5, row.isRetake());
                if (row.originalScore() != null) {
                    ps.setInt(6, row.originalScore());
                } else {
                    ps.setNull(6, java.sql.Types.INTEGER);
                }
                ps.setBoolean(7, row.isRetakeDeleted());
                ps.setTimestamp(8, Timestamp.from(createdAt));
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }
}
