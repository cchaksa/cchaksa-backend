package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.graduation.dto.CourseDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseInternalDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GraduationQueryRepositoryMapperTest {

    @Mock
    private EntityManager em;

    @Mock
    private AcademicCache academicCache;

    private GraduationQueryRepository newRepository() {
        return new GraduationQueryRepository(em, academicCache);
    }

    @Test
    @DisplayName("toCourseResponseDto 는 liberalAreaCode 정수 값을 패스스루한다")
    void toCourseResponseDto_passesNonNullLiberalAreaCodeThrough() {
        GraduationQueryRepository repository = newRepository();
        CourseInternalDto internal = new CourseInternalDto(
                1L,
                "선교",
                3,
                "A+",
                "기독교의 이해",
                10,
                2024,
                "G001",
                90,
                7
        );

        CourseDto dto = repository.toCourseResponseDto(internal);

        assertThat(dto.getLiberalAreaCode()).isEqualTo(7);
        assertThat(dto.getYear()).isEqualTo(2024);
        assertThat(dto.getCourseName()).isEqualTo("기독교의 이해");
        assertThat(dto.getCredits()).isEqualTo(3);
        assertThat(dto.getGrade()).isEqualTo("A+");
        assertThat(dto.getSemester()).isEqualTo(10);
    }

    @Test
    @DisplayName("toCourseResponseDto 는 liberalAreaCode 가 NULL 이면 그대로 null 을 유지한다")
    void toCourseResponseDto_passesNullLiberalAreaCodeThrough() {
        GraduationQueryRepository repository = newRepository();
        CourseInternalDto internal = new CourseInternalDto(
                2L,
                "전공필수",
                3,
                "B+",
                "자료구조",
                20,
                2023,
                "C001",
                85,
                null
        );

        CourseDto dto = repository.toCourseResponseDto(internal);

        assertThat(dto.getLiberalAreaCode()).isNull();
        assertThat(dto.getYear()).isEqualTo(2023);
        assertThat(dto.getCourseName()).isEqualTo("자료구조");
    }

    @Test
    @DisplayName("toCourseResponseDto 는 선교가 아닌 과목의 liberalAreaCode 를 노출하지 않는다")
    void toCourseResponseDto_omitsLiberalAreaCodeForNonMissionCourse() {
        GraduationQueryRepository repository = newRepository();
        CourseInternalDto internal = new CourseInternalDto(
                3L,
                "전핵",
                3,
                "A+",
                "자료구조",
                20,
                2024,
                "CSE101",
                95,
                7
        );

        CourseDto dto = repository.toCourseResponseDto(internal);

        assertThat(dto.getLiberalAreaCode()).isNull();
    }
}
