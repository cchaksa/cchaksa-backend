package com.chukchuk.haksa.domain.student.repository;

import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByUser(User user);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Student s SET s.targetGpa = :targetGpa WHERE s.id = :studentId")
    void updateTargetGpaByStudentId(@Param("studentId") UUID studentId, @Param("targetGpa") Double targetGpa);

    @Query("SELECT s FROM Student s " +
            "JOIN FETCH s.user u " +
            "LEFT JOIN FETCH s.department d " +
            "LEFT JOIN FETCH s.major m " +
            "WHERE s.id = :studentId")
    Optional<Student> findProfileByIdWithAssociations(@Param("studentId") UUID studentId);
}
