package com.chukchuk.haksa.domain.professor.model;

import com.chukchuk.haksa.domain.department.model.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

/** 교수 식별자와 이름을 보관하고 개설 강의와 연결한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "professor")
public class Professor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "professor_code")
  private String professorCode;

  @Column(name = "professor_name", nullable = false, unique = true)
  private String professorName;

  @CreatedDate
  @Column(name = "created_at")
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  private Department department;

  /**
   * 교수 이름으로 새 교수 엔티티를 생성한다.
   *
   * @param professorName professor 이름
   */
  public Professor(String professorName) {
    this.professorName = professorName;
  }
}
