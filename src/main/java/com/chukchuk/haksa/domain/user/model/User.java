package com.chukchuk.haksa.domain.user.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그인 계정, 프로필, 학생 연결 및 탈퇴 상태를 관리한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, unique = true)
  private UUID id;

  @Column(name = "email")
  private String email;

  @Column(name = "profile_nickname")
  private String profileNickname;

  @Column(name = "profile_image")
  private String profileImage;

  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted;

  @Column(name = "portal_connected")
  private Boolean portalConnected;

  @Column(name = "connected_at")
  private Instant connectedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt; // Soft delete 적용

  @Column(name = "last_synced_at")
  private Instant lastSyncedAt;

  @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
  private Student student;

  /**
   * 로그인 계정의 식별자·이메일·프로필 별명으로 사용자를 생성한다.
   *
   * @param id 생성할 사용자 식별자
   * @param email 연락 및 로그인에 사용하는 이메일
   * @param profileNickname 프로필 nick이름
   */
  @Builder
  public User(UUID id, String email, String profileNickname) {
    this.id = id;
    this.email = email;
    this.profileNickname = profileNickname;
    this.isDeleted = false;
    this.portalConnected = false;
  }

  public void setStudent(Student student) {
    this.student = student;
  }

  /**
   * 마지막 학사 데이터 동기화 시각을 변경한다.
   *
   * @param time 기록할 동기화 시각
   */
  public void updateLastSyncedAt(Instant time) {
    this.lastSyncedAt = time;
  }

  /**
   * 사용자의 포털 연결 완료 시각을 기록한다.
   *
   * @param now 처리 기준 시각
   */
  public void markPortalConnected(Instant now) {
    this.portalConnected = true;
    this.connectedAt = now;
    this.lastSyncedAt = now;
  }

  /**
   * 사용자를 탈퇴 상태로 전환하고 개인정보와 포털 연결 정보를 제거한다.
   *
   * @param now 탈퇴 처리 시각
   */
  public void withdraw(Instant now) {
    this.email = null;
    this.profileNickname = null;
    this.profileImage = null;
    this.isDeleted = true;
    this.portalConnected = false;
    this.connectedAt = null;
    this.deletedAt = now;
    this.lastSyncedAt = null;
  }

  /**
   * 다른 사용자 계정의 연결 정보를 현재 계정에 병합한다.
   *
   * @param origin 병합할 원본 사용자
   */
  public void absorbFrom(User origin) {
    this.email = origin.email;
    this.profileNickname = origin.profileNickname;
    this.profileImage = origin.profileImage;
    this.portalConnected = origin.portalConnected;
    this.connectedAt = origin.connectedAt;
    this.lastSyncedAt = origin.lastSyncedAt;
    this.student = origin.student;
  }
}
