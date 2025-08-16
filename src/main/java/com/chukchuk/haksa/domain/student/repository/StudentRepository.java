package com.chukchuk.haksa.domain.student.repository;

import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByUser(User user);

}
