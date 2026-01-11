package com.chukchuk.haksa.infrastructure.cache.local;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.cache.AcademicCacheKeys;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto.AcademicSummaryResponse;

@Component
@ConditionalOnProperty(name = "cache.type", havingValue = "local", matchIfMissing = true)
public class LocalAcademicCache implements AcademicCache {

    /**
     * Redis DEFAULT_TTL과 동일한 의미
     */
    private static final Duration DEFAULT_TTL = Duration.ofDays(30);

    /**
     * 주(Local) 캐시로 사용하기 위한 Caffeine 설정
     *
     * - maximumSize      : OOM 방지 (가장 중요)
     * - expireAfterWrite: Redis TTL과 동일한 의미
     * - recordStats     : 필요 시 캐시 히트율 관찰 가능
     */
    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(20_000)
            .expireAfterWrite(DEFAULT_TTL)
            .recordStats()
            .build();

    // ──────────────── Low-level helpers (Caffeine 전용) ──────────────── //

    private void put(String key, Object value) {
        cache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String key) {
        return (T) cache.getIfPresent(key);
    }

    // ──────────────── AcademicCache 구현 ──────────────── //

    @Override
    public void setAcademicSummary(UUID studentId, AcademicSummaryResponse summary) {
        put(AcademicCacheKeys.summary(studentId), summary);
    }

    @Override
    public AcademicSummaryResponse getAcademicSummary(UUID studentId) {
        return get(AcademicCacheKeys.summary(studentId));
    }

    @Override
    public void setSemesterList(
            UUID studentId,
            List<StudentSemesterDto.StudentSemesterInfoResponse> list
    ) {
        put(AcademicCacheKeys.semesters(studentId), list);
    }

    @Override
    public List<StudentSemesterDto.StudentSemesterInfoResponse> getSemesterList(UUID studentId) {
        return get(AcademicCacheKeys.semesters(studentId));
    }

    @Override
    public void setGraduationProgress(UUID studentId, GraduationProgressResponse progress) {
        put(AcademicCacheKeys.graduation(studentId), progress);
    }

    @Override
    public GraduationProgressResponse getGraduationProgress(UUID studentId) {
        return get(AcademicCacheKeys.graduation(studentId));
    }

    /**
     * 졸업요건 / 복수전공 요건은 사실상 "정적 데이터" 성격
     * → TTL은 걸려 있지만 size + LRU로 충분히 보호됨
     */
    @Override
    public void setGraduationRequirements(
            Long departmentId,
            Integer admissionYear,
            List<AreaRequirementDto> requirements
    ) {
        put(
                AcademicCacheKeys.graduationRequirements(departmentId, admissionYear),
                requirements
        );
    }

    @Override
    public List<AreaRequirementDto> getGraduationRequirements(
            Long departmentId,
            Integer admissionYear
    ) {
        return get(
                AcademicCacheKeys.graduationRequirements(departmentId, admissionYear)
        );
    }

    @Override
    public void setDualMajorRequirements(
            Long primaryMajorId,
            Long secondaryMajorId,
            Integer admissionYear,
            List<AreaRequirementDto> requirements
    ) {
        put(
                AcademicCacheKeys.dualGraduationRequirements(
                        primaryMajorId,
                        secondaryMajorId,
                        admissionYear
                ),
                requirements
        );
    }

    @Override
    public List<AreaRequirementDto> getDualMajorRequirements(
            Long primaryMajorId,
            Long secondaryMajorId,
            Integer admissionYear
    ) {
        return get(
                AcademicCacheKeys.dualGraduationRequirements(
                        primaryMajorId,
                        secondaryMajorId,
                        admissionYear
                )
        );
    }

    @Override
    public void setSemesterSummaries(
            UUID studentId,
            List<SemesterSummaryResponse> list
    ) {
        put(AcademicCacheKeys.semesterSummaries(studentId), list);
    }

    @Override
    public List<SemesterSummaryResponse> getSemesterSummaries(UUID studentId) {
        return get(AcademicCacheKeys.semesterSummaries(studentId));
    }

    /**
     * student 단위 무효화
     * - Redis의 prefix delete 대응
     * - Caffeine에서는 keySet 순회가 합리적인 선택
     */
    @Override
    public void deleteAllByStudentId(UUID studentId) {
        String prefix = AcademicCacheKeys.studentPrefix(studentId);

        cache.asMap()
                .keySet()
                .removeIf(key -> key.startsWith(prefix));
    }
}