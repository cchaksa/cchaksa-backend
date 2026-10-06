package com.chukchuk.haksa.domain.user.repository;

import com.chukchuk.haksa.domain.user.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/* JpaRepository를 확장, 커스텀 메서드 정의 */
/** 이메일·학번으로 사용자를 조회하고 프로필 연관 정보를 함께 로딩하는 저장소다. */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  /**
   * 이메일이 일치하는 사용자를 찾는다.
   *
   * @param email 사용자 이메일
   * @return 이메일이 일치하는 사용자가 있으면 포함한 선택값
   */
  Optional<User> findByEmail(String email);

  /**
   * 이메일이 등록된 사용자에게 이미 사용 중인지 확인한다.
   *
   * @param email 연락 및 로그인에 사용하는 이메일
   * @return 같은 이메일의 사용자가 있으면 {@code true}
   */
  boolean existsByEmail(String email);

  /**
   * 학번으로 연결된 사용자를 찾는다.
   *
   * @param studentCode 학번
   * @return 학번에 연결된 사용자가 있으면 포함한 선택값
   */
  Optional<User> findByStudentStudentCode(String studentCode);

  /**
   * 프로필 구성에 필요한 연관 정보를 함께 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 프로필 연관 정보를 함께 조회한 사용자가 있으면 포함한 선택값
   */
  @Query(
      """
            SELECT u FROM User u
            LEFT JOIN FETCH u.student s
            LEFT JOIN FETCH s.department
            LEFT JOIN FETCH s.major
            LEFT JOIN FETCH s.secondaryMajor
            WHERE u.id = :userId
      """)
  Optional<User> findProfileByIdWithAssociations(@Param("userId") UUID userId);
}
