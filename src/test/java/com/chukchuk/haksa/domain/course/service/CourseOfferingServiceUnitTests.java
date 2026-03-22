package com.chukchuk.haksa.domain.course.service;

import com.chukchuk.haksa.domain.course.dto.CreateOfferingCommand;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.model.LiberalArtsAreaCode;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import com.chukchuk.haksa.domain.course.repository.LiberalArtsAreaCodeRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.professor.repository.ProfessorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseOfferingServiceUnitTests {

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LiberalArtsAreaCodeRepository liberalArtsAreaCodeRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private CourseOfferingService courseOfferingService;

    @Test
    @DisplayName("동일 분반 강의가 이미 존재하면 기존 강의를 반환한다")
    void getOrCreateOffering_whenExists_returnsExisting() {
        CreateOfferingCommand cmd = command(10L, 20L, 30L, 101);
        CourseOffering existing = org.mockito.Mockito.mock(CourseOffering.class);

        when(courseOfferingRepository.findByCourseIdAndYearAndSemesterAndClassSectionAndProfessorIdAndFacultyDivisionNameAndHostDepartment(
                10L, 2024, 1, "01", 20L, FacultyDivision.전핵, "컴퓨터학과"
        )).thenReturn(Optional.of(existing));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result).isSameAs(existing);
        verify(courseOfferingRepository, never()).save(any(CourseOffering.class));
        verify(courseRepository, never()).getReferenceById(any(Long.class));
    }

    @Test
    @DisplayName("강의가 없으면 관련 참조를 조회해 새 강의를 생성/저장한다")
    void getOrCreateOffering_whenMissing_createsAndSaves() {
        CreateOfferingCommand cmd = command(11L, 21L, 31L, 202);
        Course course = new Course("CSE101", "자료구조");
        Professor professor = new Professor("홍길동");
        Department department = new Department("CS", "컴퓨터학과");
        LiberalArtsAreaCode areaCode = org.mockito.Mockito.mock(LiberalArtsAreaCode.class);

        when(courseOfferingRepository.findByCourseIdAndYearAndSemesterAndClassSectionAndProfessorIdAndFacultyDivisionNameAndHostDepartment(
                11L, 2024, 1, "01", 21L, FacultyDivision.전핵, "컴퓨터학과"
        )).thenReturn(Optional.empty());
        when(courseRepository.getReferenceById(11L)).thenReturn(course);
        when(professorRepository.getReferenceById(21L)).thenReturn(professor);
        when(departmentRepository.getReferenceById(31L)).thenReturn(department);
        when(liberalArtsAreaCodeRepository.getReferenceById(202)).thenReturn(areaCode);
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result.getCourse()).isSameAs(course);
        assertThat(result.getProfessor()).isSameAs(professor);
        assertThat(result.getDepartment()).isSameAs(department);
        assertThat(result.getLiberalArtsAreaCode()).isSameAs(areaCode);
        assertThat(result.getFacultyDivisionName()).isEqualTo(FacultyDivision.전핵);
    }

    @Test
    @DisplayName("areaCode가 null 또는 0이면 교양영역 참조를 조회하지 않는다")
    void getOrCreateOffering_withoutAreaCode_skipsLiberalArtsLookup() {
        CreateOfferingCommand cmd = command(12L, 22L, null, 0);
        Course course = new Course("MAT201", "선형대수");
        Professor professor = new Professor("김교수");

        when(courseOfferingRepository.findByCourseIdAndYearAndSemesterAndClassSectionAndProfessorIdAndFacultyDivisionNameAndHostDepartment(
                12L, 2024, 1, "01", 22L, FacultyDivision.전핵, "컴퓨터학과"
        )).thenReturn(Optional.empty());
        when(courseRepository.getReferenceById(12L)).thenReturn(course);
        when(professorRepository.getReferenceById(22L)).thenReturn(professor);
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result.getDepartment()).isNull();
        assertThat(result.getLiberalArtsAreaCode()).isNull();
        verify(liberalArtsAreaCodeRepository, never()).getReferenceById(any(Integer.class));
        verify(departmentRepository, never()).getReferenceById(any(Long.class));
    }

    @Test
    @DisplayName("offering id 목록으로 조회한 결과를 id-key map으로 반환한다")
    void getOfferingMapByIds_returnsIdMap() {
        CourseOffering first = org.mockito.Mockito.mock(CourseOffering.class);
        CourseOffering second = org.mockito.Mockito.mock(CourseOffering.class);
        when(first.getId()).thenReturn(1L);
        when(second.getId()).thenReturn(2L);
        when(courseOfferingRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        Map<Long, CourseOffering> map = courseOfferingService.getOfferingMapByIds(List.of(1L, 2L));

        assertThat(map).containsEntry(1L, first).containsEntry(2L, second);
    }

    private CreateOfferingCommand command(Long courseId, Long professorId, Long departmentId, Integer areaCode) {
        return new CreateOfferingCommand(
                courseId,
                2024,
                1,
                "01",
                professorId,
                departmentId,
                "월 1-2",
                "ABSOLUTE",
                false,
                20241,
                "전핵",
                areaCode,
                100,
                3,
                "컴퓨터학과"
        );
    }
}
