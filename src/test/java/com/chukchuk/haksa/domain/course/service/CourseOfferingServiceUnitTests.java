package com.chukchuk.haksa.domain.course.service;

import com.chukchuk.haksa.domain.course.dto.CreateOfferingCommand;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.EvaluationType;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    @DisplayName("동일 분반 강의가 이미 존재하면 기존 강의를 반환하고 누락 학점을 보정한다")
    void getOrCreateOffering_whenExists_returnsExistingAndBackfillsMissingPoints() {
        CreateOfferingCommand cmd = command(10L, 20L, 30L, 101);
        CourseOffering existing = mock(CourseOffering.class);
        Course course = mock(Course.class);
        Professor professor = mock(Professor.class);
        when(course.getId()).thenReturn(10L);
        when(professor.getId()).thenReturn(20L);
        when(existing.getCourse()).thenReturn(course);
        when(existing.getProfessor()).thenReturn(professor);
        when(existing.getYear()).thenReturn(2024);
        when(existing.getSemester()).thenReturn(1);
        when(existing.getClassSection()).thenReturn("01");
        when(existing.getFacultyDivisionName()).thenReturn(FacultyDivision.전핵);
        when(existing.getHostDepartment()).thenReturn("컴퓨터학과");

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(10L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of(existing));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result).isSameAs(existing);
        verify(existing).backfillPoints(3);
        verify(courseOfferingRepository, never()).saveAll(any());
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

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(11L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of());
        when(courseRepository.getReferenceById(11L)).thenReturn(course);
        when(professorRepository.getReferenceById(21L)).thenReturn(professor);
        when(departmentRepository.getReferenceById(31L)).thenReturn(department);
        when(liberalArtsAreaCodeRepository.getReferenceById(202)).thenReturn(areaCode);
        when(courseOfferingRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result.getCourse()).isSameAs(course);
        assertThat(result.getProfessor()).isSameAs(professor);
        assertThat(result.getDepartment()).isSameAs(department);
        assertThat(result.getLiberalArtsAreaCode()).isSameAs(areaCode);
        assertThat(result.getFacultyDivisionName()).isEqualTo(FacultyDivision.전핵);
        verify(liberalArtsAreaCodeRepository).insertIfAbsent(202, "202영역");
    }

    @Test
    @DisplayName("areaCode가 null 또는 0이면 교양영역 참조를 조회하지 않는다")
    void getOrCreateOffering_withoutAreaCode_skipsLiberalArtsLookup() {
        CreateOfferingCommand cmd = command(12L, 22L, null, 0);
        Course course = new Course("MAT201", "선형대수");
        Professor professor = new Professor("김교수");

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(12L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of());
        when(courseRepository.getReferenceById(12L)).thenReturn(course);
        when(professorRepository.getReferenceById(22L)).thenReturn(professor);
        when(courseOfferingRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result.getDepartment()).isNull();
        assertThat(result.getLiberalArtsAreaCode()).isNull();
        verify(liberalArtsAreaCodeRepository, never()).insertIfAbsent(anyInt(), anyString());
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

    @Test
    @DisplayName("평가 방식/이수구분 값이 없으면 안전한 기본값으로 저장한다")
    void getOrCreateOffering_whenEnumFieldsMissing_usesSafeDefaults() {
        CreateOfferingCommand cmd = new CreateOfferingCommand(
                13L,
                2024,
                1,
                "02",
                23L,
                null,
                "화 1-2",
                null,
                null,
                null,
                null,
                null,
                null,
                3,
                "소프트웨어학과"
        );

        Course course = new Course("SWE201", "SW공학");
        Professor professor = new Professor("이교수");

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(13L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of());
        when(courseRepository.getReferenceById(13L)).thenReturn(course);
        when(professorRepository.getReferenceById(23L)).thenReturn(professor);
        when(courseOfferingRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result.getEvaluationTypeCode()).isEqualTo(EvaluationType.UNKNOWN);
        assertThat(result.getFacultyDivisionName()).isNull();
    }

    @Test
    @DisplayName("기존 강의의 nullable 키 필드가 비어 있어도 동일 강의로 재사용한다")
    void getOrCreateOffering_whenExistingKeyFieldsAreNull_reusesExistingOffering() {
        CreateOfferingCommand cmd = new CreateOfferingCommand(
                14L,
                2024,
                1,
                " ",
                24L,
                null,
                "수 1-2",
                "ABSOLUTE",
                false,
                20241,
                " ",
                null,
                null,
                3,
                " "
        );

        CourseOffering existing = mock(CourseOffering.class);
        Course course = mock(Course.class);
        Professor professor = mock(Professor.class);
        when(course.getId()).thenReturn(14L);
        when(professor.getId()).thenReturn(24L);
        when(existing.getCourse()).thenReturn(course);
        when(existing.getProfessor()).thenReturn(professor);
        when(existing.getYear()).thenReturn(2024);
        when(existing.getSemester()).thenReturn(1);
        when(existing.getClassSection()).thenReturn(null);
        when(existing.getFacultyDivisionName()).thenReturn(null);
        when(existing.getHostDepartment()).thenReturn(null);

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(14L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of(existing));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result).isSameAs(existing);
        verify(courseOfferingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("정의되지 않은 이수 구분은 기타로 대체한다")
    void getOrCreateOffering_whenFacultyDivisionUnknown_setsEtcAndPreservesRawValue() {
        CreateOfferingCommand cmd = new CreateOfferingCommand(
                15L,
                2024,
                1,
                "01",
                25L,
                null,
                "금 3-4",
                "ABSOLUTE",
                false,
                20241,
                "신규구분",
                null,
                null,
                3,
                "컴퓨터학과"
        );

        Course course = new Course("GEN101", "일반과목");
        Professor professor = new Professor("박교수");

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(15L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of());
        when(courseRepository.getReferenceById(15L)).thenReturn(course);
        when(professorRepository.getReferenceById(25L)).thenReturn(professor);
        when(courseOfferingRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result.getFacultyDivisionName()).isEqualTo(FacultyDivision.기타);
        assertThat(result.getRawFacultyDivisionName()).isEqualTo("신규구분");
    }

    @Test
    @DisplayName("정의된 이수 구분은 원본 문자열을 별도로 저장하지 않는다")
    void getOrCreateOffering_whenFacultyDivisionKnown_doesNotPreserveRawValue() {
        CreateOfferingCommand cmd = command(16L, 26L, null, null);
        Course course = new Course("CSE202", "운영체제");
        Professor professor = new Professor("정교수");

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(16L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of());
        when(courseRepository.getReferenceById(16L)).thenReturn(course);
        when(professorRepository.getReferenceById(26L)).thenReturn(professor);
        when(courseOfferingRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result.getFacultyDivisionName()).isEqualTo(FacultyDivision.전핵);
        assertThat(result.getRawFacultyDivisionName()).isNull();
    }

    @Test
    @DisplayName("동일한 미지원 이수 구분은 재연동 시 기존 기타 강의를 재사용한다")
    void getOrCreateOffering_whenExistingEtcWithSameRawValue_reusesExistingOffering() {
        CreateOfferingCommand cmd = new CreateOfferingCommand(
                17L,
                2024,
                1,
                "01",
                27L,
                null,
                "월 1-2",
                "ABSOLUTE",
                false,
                20241,
                "RT",
                null,
                null,
                2,
                "컴퓨터학과"
        );

        CourseOffering existing = mock(CourseOffering.class);
        Course course = mock(Course.class);
        Professor professor = mock(Professor.class);
        when(course.getId()).thenReturn(17L);
        when(professor.getId()).thenReturn(27L);
        when(existing.getCourse()).thenReturn(course);
        when(existing.getProfessor()).thenReturn(professor);
        when(existing.getYear()).thenReturn(2024);
        when(existing.getSemester()).thenReturn(1);
        when(existing.getClassSection()).thenReturn("01");
        when(existing.getFacultyDivisionName()).thenReturn(FacultyDivision.기타);
        when(existing.getRawFacultyDivisionName()).thenReturn("RT");
        when(existing.getHostDepartment()).thenReturn("컴퓨터학과");

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(17L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of(existing));

        CourseOffering result = courseOfferingService.getOrCreateOffering(cmd);

        assertThat(result).isSameAs(existing);
        verify(courseOfferingRepository, never()).saveAll(any());
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

    // ====================================================================
    // Issue #226 2차 작업 — 재스크래핑 시 선교 영역 area_code 단방향 backfill
    // ====================================================================

    @Test
    @DisplayName("T1 backfill: 선교 + area_code null + cmd.areaCode=6 → backfillMissionLiberalAreaCode 호출됨")
    void getOrCreateAll_missionAndAreaCodeNull_backfillsFromCmd() {
        CreateOfferingCommand cmd = missionCommand(50L, 60L, 6);
        CourseOffering existing = missionExistingMock(50L, 60L, null);
        LiberalArtsAreaCode areaProxy = mock(LiberalArtsAreaCode.class);

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(50L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of(existing));
        when(liberalArtsAreaCodeRepository.getReferenceById(6)).thenReturn(areaProxy);

        courseOfferingService.getOrCreateOffering(cmd);

        verify(existing, times(1)).backfillMissionLiberalAreaCode(areaProxy);
        verify(courseOfferingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("T2 backfill: 선교 + area_code 이미 존재 → backfill 호출 안 됨")
    void getOrCreateAll_missionAndAreaCodeAlreadyPresent_skipsBackfill() {
        CreateOfferingCommand cmd = missionCommand(51L, 61L, 6);
        LiberalArtsAreaCode existingArea = mock(LiberalArtsAreaCode.class);
        CourseOffering existing = missionExistingMock(51L, 61L, existingArea);

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(51L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of(existing));

        courseOfferingService.getOrCreateOffering(cmd);

        verify(existing, never()).backfillMissionLiberalAreaCode(any());
        verify(liberalArtsAreaCodeRepository, never()).getReferenceById(any(Integer.class));
    }

    @Test
    @DisplayName("T3 backfill: 비-선교(전핵) cmd + 비-선교 existing → backfill 호출 안 됨")
    void getOrCreateAll_nonMissionFacultyDivision_skipsBackfill() {
        // 키가 매칭되어 기존 row reuse 가 되도록 cmd·existing 모두 비-선교(전핵)로 정렬
        CreateOfferingCommand cmd = command(52L, 62L, null, 6); // facultyDivisionName="전핵"
        CourseOffering existing = mock(CourseOffering.class);
        Course course = mock(Course.class);
        Professor professor = mock(Professor.class);
        when(course.getId()).thenReturn(52L);
        when(professor.getId()).thenReturn(62L);
        when(existing.getCourse()).thenReturn(course);
        when(existing.getProfessor()).thenReturn(professor);
        when(existing.getYear()).thenReturn(2024);
        when(existing.getSemester()).thenReturn(1);
        when(existing.getClassSection()).thenReturn("01");
        when(existing.getFacultyDivisionName()).thenReturn(FacultyDivision.전핵);
        when(existing.getHostDepartment()).thenReturn("컴퓨터학과");

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(52L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of(existing));

        courseOfferingService.getOrCreateOffering(cmd);

        verify(existing, never()).backfillMissionLiberalAreaCode(any());
        verify(liberalArtsAreaCodeRepository, never()).getReferenceById(any(Integer.class));
    }

    @Test
    @DisplayName("T4 backfill: 선교 + area_code null + cmd.areaCode=null → backfill 호출 안 됨")
    void getOrCreateAll_missionAndCmdAreaCodeNull_skipsBackfill() {
        CreateOfferingCommand cmd = missionCommand(53L, 63L, null);
        CourseOffering existing = missionExistingMock(53L, 63L, null);

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(53L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of(existing));

        courseOfferingService.getOrCreateOffering(cmd);

        verify(existing, never()).backfillMissionLiberalAreaCode(any());
        verify(liberalArtsAreaCodeRepository, never()).getReferenceById(any(Integer.class));
    }

    @Test
    @DisplayName("T5 backfill: 선교 + area_code null + cmd.areaCode=0 → backfill 호출 안 됨")
    void getOrCreateAll_missionAndCmdAreaCodeZero_skipsBackfill() {
        CreateOfferingCommand cmd = missionCommand(54L, 64L, 0);
        CourseOffering existing = missionExistingMock(54L, 64L, null);

        when(courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(
                Set.of(54L), Set.of(2024), Set.of(1)
        )).thenReturn(List.of(existing));

        courseOfferingService.getOrCreateOffering(cmd);

        verify(existing, never()).backfillMissionLiberalAreaCode(any());
        verify(liberalArtsAreaCodeRepository, never()).getReferenceById(any(Integer.class));
    }

    // --- helpers (mission backfill) ---

    private CreateOfferingCommand missionCommand(Long courseId, Long professorId, Integer areaCode) {
        return new CreateOfferingCommand(
                courseId,
                2024,
                1,
                "01",
                professorId,
                null,
                "월 1-2",
                "ABSOLUTE",
                false,
                20241,
                "선교",
                areaCode,
                100,
                3,
                "교양학과"
        );
    }

    private CourseOffering missionExistingMock(Long courseId, Long professorId, LiberalArtsAreaCode currentArea) {
        CourseOffering existing = mock(CourseOffering.class);
        Course course = mock(Course.class);
        Professor professor = mock(Professor.class);
        when(course.getId()).thenReturn(courseId);
        when(professor.getId()).thenReturn(professorId);
        when(existing.getCourse()).thenReturn(course);
        when(existing.getProfessor()).thenReturn(professor);
        when(existing.getYear()).thenReturn(2024);
        when(existing.getSemester()).thenReturn(1);
        when(existing.getClassSection()).thenReturn("01");
        when(existing.getFacultyDivisionName()).thenReturn(FacultyDivision.선교);
        when(existing.getHostDepartment()).thenReturn("교양학과");
        when(existing.getLiberalArtsAreaCode()).thenReturn(currentArea);
        return existing;
    }
}
