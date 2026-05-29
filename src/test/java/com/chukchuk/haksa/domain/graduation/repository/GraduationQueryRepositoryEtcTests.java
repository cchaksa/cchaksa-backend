package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseInternalDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GraduationQueryRepositoryEtcTests {

    private final GraduationQueryRepository repository = new TestGraduationQueryRepository();

    @Test
    @DisplayName("기타 이수구분 수강 이력이 있으면 매직 넘버 영역을 추가한다")
    void getStudentAreaProgress_addsEtcAreaWithMagicNumbers() {
        List<AreaProgressDto> progress = repository.getStudentAreaProgress(UUID.randomUUID(), 1L, 2024);

        AreaProgressDto etcArea = progress.stream()
                .filter(dto -> dto.getAreaType() == FacultyDivision.기타)
                .findFirst()
                .orElseThrow();

        assertThat(etcArea.getRequiredCredits()).isZero();
        assertThat(etcArea.getEarnedCredits()).isEqualTo(3);
        assertThat(etcArea.getCourses())
                .map(CourseDto::getCourseName)
                .containsExactlyInAnyOrder("특별활동", "미지정활동");
    }

    private static class TestGraduationQueryRepository extends GraduationQueryRepository {

        TestGraduationQueryRepository() {
            super(null, mock(AcademicCache.class));
        }

        @Override
        public List<AreaRequirementDto> getAreaRequirementsWithCache(Long deptId, Integer admissionYear) {
            return List.of(new AreaRequirementDto("전핵", 12, 1, 2));
        }

        @Override
        public List<CourseInternalDto> getLatestValidCourses(UUID studentId) {
            CourseInternalDto major = new CourseInternalDto(
                    1L, "전핵", 3, "A+", "자료구조", 1, 2024, "CSE101", 95
            );
            CourseInternalDto etc = new CourseInternalDto(
                    2L, FacultyDivision.기타.name(), 1, "P", "특별활동", 1, 2024, "ETC001", 0
            );
            CourseInternalDto undefined = new CourseInternalDto(
                    3L, null, 2, "P", "미지정활동", 1, 2024, "UNK001", 0
            );
            return List.of(major, etc, undefined);
        }
    }
}
