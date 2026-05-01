package com.chukchuk.haksa.domain.user.repository;

import com.chukchuk.haksa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/* JpaRepository를 확장, 커스텀 메서드 정의 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByStudent_StudentCode(String studentCode);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.student s
            LEFT JOIN FETCH s.department
            LEFT JOIN FETCH s.major
            LEFT JOIN FETCH s.secondaryMajor
            WHERE u.id = :userId
            """)
    Optional<User> findProfileByIdWithAssociations(@Param("userId") UUID userId);
}
