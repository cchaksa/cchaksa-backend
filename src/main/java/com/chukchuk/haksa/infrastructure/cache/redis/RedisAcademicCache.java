package com.chukchuk.haksa.infrastructure.cache.redis;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.cache.AcademicCacheKeys;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto.AcademicSummaryResponse;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cache.type", havingValue = "redis")
public class RedisAcademicCache implements AcademicCache {

    private static final Duration DEFAULT_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper ob;

    // ──────────────── Low-level helpers (Redis 전용) ──────────────── //

    private <T> void set(String key, T value, Duration ttl) {
        try {
            redisTemplate.opsForValue()
                    .set(key, ob.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 캐싱 직렬화 실패", e);
        }
    }

    private <T> void setPermanent(String key, T value) {
        try {
            redisTemplate.opsForValue()
                    .set(key, ob.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 캐싱 직렬화 실패", e);
        }
    }

    private <T> T get(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            return ob.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private <T> List<T> getList(String key, Class<T> elementClass) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            JavaType type = ob.getTypeFactory()
                    .constructCollectionType(List.class, elementClass);
            return ob.readValue(json, type);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void deleteByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ──────────────── AcademicCache 구현 ──────────────── //

    @Override
    public void setAcademicSummary(UUID studentId, AcademicSummaryResponse summary) {
        set(AcademicCacheKeys.summary(studentId), summary, DEFAULT_TTL);
    }

    @Override
    public AcademicSummaryResponse getAcademicSummary(UUID studentId) {
        return get(AcademicCacheKeys.summary(studentId), AcademicSummaryResponse.class);
    }

    @Override
    public void setSemesterList(UUID studentId, List<StudentSemesterDto.StudentSemesterInfoResponse> list) {
        set(AcademicCacheKeys.semesters(studentId), list, DEFAULT_TTL);
    }

    @Override
    public List<StudentSemesterDto.StudentSemesterInfoResponse> getSemesterList(UUID studentId) {
        return getList(
                AcademicCacheKeys.semesters(studentId),
                StudentSemesterDto.StudentSemesterInfoResponse.class
        );
    }

    @Override
    public void setGraduationProgress(UUID studentId, GraduationProgressResponse progress) {
        set(AcademicCacheKeys.graduation(studentId), progress, DEFAULT_TTL);
    }

    @Override
    public GraduationProgressResponse getGraduationProgress(UUID studentId) {
        return get(AcademicCacheKeys.graduation(studentId), GraduationProgressResponse.class);
    }

    @Override
    public void setGraduationRequirements(Long departmentId, Integer admissionYear, List<AreaRequirementDto> requirements) {
        setPermanent(
                AcademicCacheKeys.graduationRequirements(departmentId, admissionYear),
                requirements
        );
    }

    @Override
    public List<AreaRequirementDto> getGraduationRequirements(Long departmentId, Integer admissionYear) {
        return getList(
                AcademicCacheKeys.graduationRequirements(departmentId, admissionYear),
                AreaRequirementDto.class
        );
    }

    @Override
    public void setDualMajorRequirements(Long primaryMajorId, Long secondaryMajorId, Integer admissionYear, List<AreaRequirementDto> requirements) {
        setPermanent(
                AcademicCacheKeys.dualGraduationRequirements(primaryMajorId, secondaryMajorId, admissionYear),
                requirements
        );
    }

    @Override
    public List<AreaRequirementDto> getDualMajorRequirements(Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
        return getList(
                AcademicCacheKeys.dualGraduationRequirements(primaryMajorId, secondaryMajorId, admissionYear),
                AreaRequirementDto.class
        );
    }

    @Override
    public void setSemesterSummaries(UUID studentId, List<SemesterSummaryResponse> list) {
        set(AcademicCacheKeys.semesterSummaries(studentId), list, DEFAULT_TTL);
    }

    @Override
    public List<SemesterSummaryResponse> getSemesterSummaries(UUID studentId) {
        return getList(
                AcademicCacheKeys.semesterSummaries(studentId),
                SemesterSummaryResponse.class
        );
    }

    @Override
    public void deleteAllByStudentId(UUID studentId) {
        deleteByPrefix(AcademicCacheKeys.studentPrefix(studentId));
    }
}