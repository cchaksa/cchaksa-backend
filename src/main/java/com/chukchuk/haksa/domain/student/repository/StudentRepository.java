package com.chukchuk.haksa.domain.student.repository;

import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.user.model.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 학생을 식별 정보로 조회하고 프로필 상태를 갱신하는 저장소이다. */
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
  /**
   * 사용자에게 연결된 학생을 찾는다.
   *
   * @param user 학생 연결을 조회할 사용자
   * @return 연결된 학생이 있으면 포함한 선택값
   */
  Optional<Student> findByUser(User user);

  /**
   * 학생 식별자로 학생 행을 쓰기 잠금과 함께 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 잠근 학생이 있으면 포함한 선택값
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Student s WHERE s.id = :studentId")
  Optional<Student> findForUpdateById(@Param("studentId") UUID studentId);

  /**
   * 사용자 식별자로 학생 행을 쓰기 잠금과 함께 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 잠근 학생이 있으면 포함한 선택값
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Student s WHERE s.user.id = :userId")
  Optional<Student> findForUpdateByUserId(@Param("userId") UUID userId);

  /**
   * 학번이 일치하는 학생을 찾는다.
   *
   * @param studentCode 학번
   * @return 학생이 있으면 포함한 선택값
   */
  Optional<Student> findByStudentCode(String studentCode);

  /**
   * 학생의 목표 평점을 변경하고 영속성 컨텍스를 즉시 동기화한다.
   *
   * @param studentId 학생 식별자
   * @param targetGpa 새 목표 평점
   */
  @Modifying(clearAutomatically = true)
  @Query("UPDATE Student s SET s.targetGpa = :targetGpa WHERE s.id = :studentId")
  void updateTargetGpaByStudentId(
      @Param("studentId") UUID studentId, @Param("targetGpa") Double targetGpa);

  /**
   * 학생 프로필에 필요한 사용자·학과·전공을 함께 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 연관 정보가 로딩된 학생이 있으면 포함한 선택값
   */
  @Query(
      "SELECT s FROM Student s "
          + "JOIN FETCH s.user u "
          + "LEFT JOIN FETCH s.department d "
          + "LEFT JOIN FETCH s.major m "
          + "WHERE s.id = :studentId")
  Optional<Student> findProfileByIdWithAssociations(@Param("studentId") UUID studentId);

  /**
   * 포털 연동이 완료되지 않은 사용자의 학생을 찾는다.
   *
   * @param userId 사용자 식별자
   * @return 연동 대기 학생이 있으면 포함한 선택값
   */
  @Query(
      "SELECT s FROM Student s JOIN s.user u WHERE u.id = :userId AND "
          + "(u.portalConnected = false OR u.portalConnected IS NULL)")
  Optional<Student> findPortalPendingStudent(@Param("userId") UUID userId);
}
