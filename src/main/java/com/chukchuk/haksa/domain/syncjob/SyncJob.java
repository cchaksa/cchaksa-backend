package com.chukchuk.haksa.domain.syncjob;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "sync_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 32)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", nullable = false, length = 32)
    private SyncPhase currentPhase;

    private SyncJob(UUID userId, JobType jobType) {
        this.userId = userId;
        this.jobType = jobType;
        this.status = JobStatus.INITIALIZED;
        this.currentPhase = SyncPhase.FETCHING;
    }

    public static SyncJob create(UUID userId, JobType jobType) {
        return new SyncJob(userId, jobType);
    }

    public boolean isTerminal() {
        return this.status == JobStatus.SUCCESS || this.status == JobStatus.FAIL;
    }

    public boolean canProcess() {
        return this.status == JobStatus.INITIALIZED;
    }

    public void updateStatus(JobStatus status, SyncPhase phase) {
        this.status = status;
        this.currentPhase = phase;
    }
}
