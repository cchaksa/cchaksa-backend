package com.chukchuk.haksa.infrastructure.redis;

import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto.AcademicSummaryResponse;

@Component
@RequiredArgsConstructor
public class RedisCacheStore {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper ob;

    public static final Duration DEFAULT_TTL = Duration.ofDays(30);

    // 기본 저장: Default TTL 사용
    public <T> void set(String key, T value) {
        try {
            String json = ob.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 캐싱 직렬화 실패", e);
        }
    }

    // 저장: TTL 수동 설정
    public <T> void set(String key, T value, Duration ttl) {
        try {
            String json = ob.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 캐싱 직렬화 실패", e);
        }
    }

    // 단건 조회
    public <T> T get(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            return ob.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // 복수 조회
    public <T> List<T> getList(String key, Class<T>  elementClass) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            JavaType type = ob.getTypeFactory().constructCollectionType(List.class, elementClass);
            return ob.readValue(json, type);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // 단건 삭제
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // prefix로 여러 키 삭제
    public void deleteByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }

    // studentId 기반 전체 캐시 삭제
    public void deleteAllByStudentId(UUID studentId) {
        String prefix = "student:" + studentId + ":";
        deleteByPrefix(prefix);
    }

    // ──────────────── [도메인별 key 헬퍼] ──────────────── //

    public String keyForSummary(UUID studentId) {
        return "student:" + studentId + ":summary";
    }

    public String keyForSemesters(UUID studentId) {
        return "student:" + studentId + ":semesters";
    }

    public String keyForGraduation(UUID studentId) {
        return "student:" + studentId + ":graduation";
    }

    public String keyForTotalRequiredGraduationCredits(Long departmentId, Integer admissionYear) { return "graduation:" + departmentId + ":" + admissionYear;}

    // ──────────────── [도메인별 캐시 처리] ──────────────── //

    public void setAcademicSummary(UUID studentId, AcademicSummaryResponse summary) {
        set(keyForSummary(studentId), summary);
    }

    public AcademicSummaryResponse getAcademicSummary(UUID studentId) {
        return get(keyForSummary(studentId), AcademicSummaryResponse.class);
    }

    public void setSemesterList(UUID studentId, List<StudentSemesterDto.StudentSemesterInfoResponse> list) {
        set(keyForSemesters(studentId), list);
    }

    public List<StudentSemesterDto.StudentSemesterInfoResponse> getSemesterList(UUID studentId) {
        return getList(keyForSemesters(studentId), StudentSemesterDto.StudentSemesterInfoResponse.class);
    }

    public void setGraduationProgress(UUID studentId, GraduationProgressResponse progress) {
        set(keyForGraduation(studentId), progress);
    }

    public GraduationProgressResponse getGraduationProgress(UUID studentId) {
        return get(keyForGraduation(studentId), GraduationProgressResponse.class);
    }

    public void setTotalRequiredGraduationCredits(Long departmentId, Integer admissionYear, Integer graduationCredits) {
        set(keyForTotalRequiredGraduationCredits(departmentId, admissionYear), graduationCredits);
    }

    public Integer getTotalRequiredGraduationCredits(Long departmentId, Integer admissionYear) {
        return get(keyForTotalRequiredGraduationCredits(departmentId, admissionYear), Integer.class);
    }
}
