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

        AreaProgressDto majorArea = progress.stream()
                .filter(dto -> dto.getAreaType() == FacultyDivision.전핵)
                .findFirst()
                .orElseThrow();
        AreaProgressDto etcArea = progress.stream()
                .filter(dto -> dto.getAreaType() == FacultyDivision.기타)
                .findFirst()
                .orElseThrow();

        assertThat(majorArea.getCompletedElectiveCourses()).isEqualTo(1);
        assertThat(etcArea.getRequiredCredits()).isZero();
        assertThat(etcArea.getEarnedCredits()).isEqualTo(3);
        assertThat(etcArea.getCourses())
                .map(CourseDto::getCourseName)
                .containsExactlyInAnyOrder("특별활동", "미지정활동");
    }

    @Test
    @DisplayName("선교 completedElectiveCourses 는 null 을 제외한 세부 영역 고유 개수로 계산한다")
    void getStudentAreaProgress_countsMissionDistinctLiberalAreas() {
        GraduationQueryRepository missionRepository = new TestMissionGraduationQueryRepository();

        List<AreaProgressDto> progress = missionRepository.getStudentAreaProgress(UUID.randomUUID(), 1L, 2024);

        AreaProgressDto missionArea = progress.stream()
                .filter(dto -> dto.getAreaType() == FacultyDivision.선교)
                .findFirst()
                .orElseThrow();

        assertThat(missionArea.getCompletedElectiveCourses()).isEqualTo(2);
        assertThat(missionArea.getCourses())
                .map(CourseDto::getCourseName)
                .containsExactlyInAnyOrder("세계차산업의이해", "일본사회의이해", "러시아문화", "영역미확인선교");
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
                    1L, "전핵", 3, "A+", "자료구조", 1, 2024, "CSE101", 95, null
            );
            CourseInternalDto etc = new CourseInternalDto(
                    2L, FacultyDivision.기타.name(), 1, "P", "특별활동", 1, 2024, "ETC001", 0, null
            );
            CourseInternalDto undefined = new CourseInternalDto(
                    3L, null, 2, "P", "미지정활동", 1, 2024, "UNK001", 0, null
            );
            return List.of(major, etc, undefined);
        }
    }

    private static class TestMissionGraduationQueryRepository extends GraduationQueryRepository {

        TestMissionGraduationQueryRepository() {
            super(null, mock(AcademicCache.class));
        }

        @Override
        public List<AreaRequirementDto> getAreaRequirementsWithCache(Long deptId, Integer admissionYear) {
            return List.of(new AreaRequirementDto("선교", 18, 6, 7));
        }

        @Override
        public List<CourseInternalDto> getLatestValidCourses(UUID studentId) {
            CourseInternalDto area2First = new CourseInternalDto(
                    1L, "선교", 3, "A0", "세계차산업의이해", 203, 2024, "CUL201", 90, 2
            );
            CourseInternalDto area2Second = new CourseInternalDto(
                    2L, "선교", 3, "A+", "일본사회의이해", 10, 2024, "CUL202", 95, 2
            );
            CourseInternalDto area4 = new CourseInternalDto(
                    3L, "선교", 3, "A0", "러시아문화", 10, 2026, "CUL401", 90, 4
            );
            CourseInternalDto unknownArea = new CourseInternalDto(
                    4L, "선교", 3, "B+", "영역미확인선교", 20, 2025, "CUL999", 85, null
            );
            return List.of(area2First, area2Second, area4, unknownArea);
        }
    }
}
