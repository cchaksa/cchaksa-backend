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
    private FacultyDivision facultyDivisionName;

    @Column(name = "raw_faculty_division_name", length = 64)
    private String rawFacultyDivisionName;
    
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
        this(
                subjectEstablishmentSemester,
                isVideoLecture,
                year,
                semester,
                hostDepartment,
                classSection,
                scheduleSummary,
                originalAreaCode,
                points,
                evaluationTypeCode,
                facultyDivisionName,
                null,
                course,
                professor,
                department,
                liberalArtsAreaCode
        );
    }

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
            String rawFacultyDivisionName,
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
        this.rawFacultyDivisionName = rawFacultyDivisionName;
        this.course = course;
        this.professor = professor;
        this.department = department;
        this.liberalArtsAreaCode = liberalArtsAreaCode;
    }
}
