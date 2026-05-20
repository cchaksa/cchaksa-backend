package com.chukchuk.haksa.domain.course.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.professor.model.Professor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_offerings")
public class CourseOffering extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_establishment_semester")
    private Integer subjectEstablishmentSemester;

    @Column(name = "is_video_lecture")
    private Boolean isVideoLecture;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "host_department")
    private String hostDepartment;

    @Column(name = "class_section")
    private String classSection;

    @Column(name = "schedule_summary")
    private String scheduleSummary;

    @Column(name = "original_area_code")
    private Integer originalAreaCode;

    @Column(name = "points")
    private Integer points;

    @Column(name = "deleted_at")
    private Instant deletedAt; // Soft delete 적용

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type_code")
    private EvaluationType evaluationTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "faculty_division_name")
    private  FacultyDivision facultyDivisionName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_code", referencedColumnName = "code")
    private LiberalArtsAreaCode liberalArtsAreaCode;

    public CourseOffering(
            Integer subjectEstablishmentSemester,
            Boolean isVideoLecture,
            Integer year,
            Integer semester,
            String hostDepartment,
            String classSection,
            String scheduleSummary,
            Integer originalAreaCode,
            Integer points,
            EvaluationType evaluationTypeCode,
            FacultyDivision facultyDivisionName,
            Course course,
            Professor professor,
            Department department,
            LiberalArtsAreaCode liberalArtsAreaCode
    ) {
        this.subjectEstablishmentSemester = subjectEstablishmentSemester;
        this.isVideoLecture = isVideoLecture;
        this.year = year;
        this.semester = semester;
        this.hostDepartment = hostDepartment;
        this.classSection = classSection;
        this.scheduleSummary = scheduleSummary;
        this.originalAreaCode = originalAreaCode;
        this.points = points;
        this.evaluationTypeCode = evaluationTypeCode;
        this.facultyDivisionName = facultyDivisionName;
        this.course = course;
        this.professor = professor;
        this.department = department;
        this.liberalArtsAreaCode = liberalArtsAreaCode;
    }

    /**
     * 선교(미션) 영역의 historical NULL area_code 전용 backfill 채널 (Issue #226 2차 작업).
     *
     * <p><b>용도 한정</b>: 이 메서드는 재스크래핑(첫 연결/재연결) 흐름에서 기존 row의
     * {@code area_code}가 NULL인 선교 과목만 단방향(NULL → non-null) 채우기 위해 도입되었다.
     * CourseOffering의 다른 필드는 본 메서드로도 변경되지 않으며, 다른 영역/다른 필드에
     * 동일한 패턴을 적용해야 한다면 이 메서드를 재사용하지 말고 별도 채널을 도입해야 한다.
     *
     * <p><b>내부 가드 (fail-fast)</b>:
     * <ul>
     *   <li>{@code facultyDivisionName != FacultyDivision.선교} → {@link IllegalStateException}</li>
     *   <li>이미 {@code liberalArtsAreaCode != null} → idempotent no-op (return)</li>
     *   <li>인자 {@code area == null} → {@link IllegalArgumentException}</li>
     * </ul>
     *
     * @param area 새로 부여할 LiberalArtsAreaCode (non-null, 단 호출 시점에 not-managed proxy 가능).
     * @throws IllegalStateException    선교가 아닌 영역에서 호출된 경우
     * @throws IllegalArgumentException {@code area}가 null인 경우
     */
    public void backfillMissionLiberalAreaCode(LiberalArtsAreaCode area) {
        if (this.facultyDivisionName != FacultyDivision.선교) {
            throw new IllegalStateException(
                    "backfillMissionLiberalAreaCode 는 선교 영역에서만 호출 가능합니다. current="
                            + this.facultyDivisionName
            );
        }
        if (this.liberalArtsAreaCode != null) {
            return; // idempotent: 이미 backfill 됨
        }
        if (area == null) {
            throw new IllegalArgumentException(
                    "backfill 대상 LiberalArtsAreaCode 는 null 일 수 없습니다."
            );
        }
        this.liberalArtsAreaCode = area;
    }
}
