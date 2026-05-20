package com.chukchuk.haksa.domain.course.model;

import com.chukchuk.haksa.domain.course.repository.LiberalArtsAreaCodeRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.professor.model.Professor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * {@link CourseOffering#backfillMissionLiberalAreaCode(LiberalArtsAreaCode)} 의 4겹 방어선 검증.
 * <p>Issue #226 2차 작업의 도메인 가드 (선교 검증, idempotent, null 검증)가 의도대로 동작하는지
 * 단위 수준에서 직접 확인한다. {@link LiberalArtsAreaCodeRepository}는 사용하지 않으므로 가벼운 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class CourseOfferingBackfillTest {

    @Test
    @DisplayName("선교 영역 + area null 인 경우 정상적으로 채워진다")
    void backfill_onMissionWithNull_assignsArea() {
        CourseOffering offering = newOffering(FacultyDivision.선교, null);
        LiberalArtsAreaCode area = mock(LiberalArtsAreaCode.class);

        offering.backfillMissionLiberalAreaCode(area);

        assertThat(offering.getLiberalArtsAreaCode()).isSameAs(area);
    }

    @Test
    @DisplayName("선교 영역인데 이미 area 가 있으면 idempotent 하게 무시된다")
    void backfill_onMissionAlreadyAssigned_isNoOp() {
        LiberalArtsAreaCode existing = mock(LiberalArtsAreaCode.class);
        CourseOffering offering = newOffering(FacultyDivision.선교, existing);
        LiberalArtsAreaCode incoming = mock(LiberalArtsAreaCode.class);

        offering.backfillMissionLiberalAreaCode(incoming);

        assertThat(offering.getLiberalArtsAreaCode()).isSameAs(existing);
    }

    @Test
    @DisplayName("선교 외 영역에서 호출하면 IllegalStateException 으로 즉시 차단된다")
    void backfill_onNonMission_throwsIllegalState() {
        CourseOffering offering = newOffering(FacultyDivision.전핵, null);
        LiberalArtsAreaCode area = mock(LiberalArtsAreaCode.class);

        assertThatThrownBy(() -> offering.backfillMissionLiberalAreaCode(area))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("선교 영역에서만");
    }

    @Test
    @DisplayName("선교 영역인데 인자 area 가 null 이면 IllegalArgumentException")
    void backfill_withNullArea_throwsIllegalArgument() {
        CourseOffering offering = newOffering(FacultyDivision.선교, null);

        assertThatThrownBy(() -> offering.backfillMissionLiberalAreaCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CourseOffering newOffering(FacultyDivision division, LiberalArtsAreaCode initialArea) {
        Course course = mock(Course.class);
        Professor professor = mock(Professor.class);
        Department department = mock(Department.class);
        return new CourseOffering(
                20241,
                false,
                2024,
                1,
                "교양학과",
                "01",
                "월 1-2",
                null,
                3,
                EvaluationType.ABSOLUTE,
                division,
                course,
                professor,
                department,
                initialArea
        );
    }
}
