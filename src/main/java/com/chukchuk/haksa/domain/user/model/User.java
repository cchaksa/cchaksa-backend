package com.chukchuk.haksa.domain.user.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(name = "email", nullable = false)
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

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Student student;

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

    public void updateLastSyncedAt(Instant time) {
        this.lastSyncedAt = time;
    }

    public void markPortalConnected(Instant now) {
        this.portalConnected = true;
        this.connectedAt = now;
        this.lastSyncedAt = now;
    }

    public void absorbFrom(User origin) {
        this.email = origin.email;
        this.profileNickname = origin.profileNickname;
        this.profileImage = origin.profileImage;
        this.isDeleted = origin.isDeleted;
        this.portalConnected = origin.portalConnected;
        this.connectedAt = origin.connectedAt;
        this.deletedAt = origin.deletedAt;
        this.lastSyncedAt = origin.lastSyncedAt;
        this.student = origin.student;
    }
}
