package com.chukchuk.haksa.domain.student.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.model.StudentGraduationProgress;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.user.model.StudentInitializationDataType;
import com.chukchuk.haksa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.REMOVE;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "students")
public class Student extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "student_id")
    private UUID id;

    @Column(name = "student_code", nullable = false, unique = true)
    private String studentCode;

    @Column(name = "name")
    private String name;

    @Column(name = "is_graduated")
    private Boolean isGraduated;

    @Column(name = "admission_type")
    private String admissionType;

    @Column(name = "target_gpa")
    private Double targetGpa;

    @Embedded
    private AcademicInfo academicInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    private Department major;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_major_id")
    private Department secondaryMajor;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private StudentAcademicRecord studentAcademicRecord;

    @OneToOne(mappedBy = "student", cascade = {CascadeType.PERSIST, REMOVE}, orphanRemoval = true)
    private StudentGraduationProgress graduationProgress;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SemesterAcademicRecord> semesterAcademicRecords = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = {CascadeType.PERSIST, REMOVE}, orphanRemoval = true)
    private List<StudentCourse> studentCourses = new ArrayList<>();

    /* Using Method */

    // 회원 정보 초기화
    public void resetAcademicData() {
        // 학기별 성적 기록 삭제
        this.semesterAcademicRecords.clear();

        // 수강 과목 기록 삭제
        this.studentCourses.clear();

        // 누적 성적 기록 삭제
        this.studentAcademicRecord = null;

//        // 필요한 경우 학업 정보도 초기화
//        if (this.academicInfo != null) {
//            this.academicInfo = AcademicInfo.builder()
//                    .admissionYear(this.academicInfo.getAdmissionYear())
//                    .semesterEnrolled(this.academicInfo.getSemesterEnrolled())
//                    .isTransferStudent(this.academicInfo.getIsTransferStudent())
//                    .status(StudentStatus.ENROLLED) // 예: 기본값
//                    .gradeLevel(1)
//                    .completedSemesters(0)
//                    .build();
//        }
    }

    @Builder
    public Student(String studentCode, String name, Department department, Department major, Department secondaryMajor,
                   Integer admissionYear, Integer semesterEnrolled, Boolean isTransferStudent, Boolean isGraduated,
                   StudentStatus status, Integer gradeLevel, Integer completedSemesters, String admissionType, User user) {
        this.studentCode = studentCode;
        this.name = name;
        this.department = department;
        this.major = major;
        this.secondaryMajor = secondaryMajor;
        this.isGraduated = isGraduated;
        this.admissionType = admissionType;

        // Builder 패턴을 이용해 AcademicInfo 객체 생성
        this.academicInfo = AcademicInfo.builder()
                .admissionYear(admissionYear)
                .semesterEnrolled(semesterEnrolled)
                .isTransferStudent(isTransferStudent)
                .status(status)  // Enum으로 변환
                .gradeLevel(gradeLevel)
                .completedSemesters(completedSemesters)
                .build();

        this.user = user;
    }

    public void updateInfo(String name, Department department, Department major, Department secondaryMajor,
                           Integer admissionYear, Integer semesterEnrolled, Boolean isTransferStudent,
                           Boolean isGraduated, StudentStatus status, Integer gradeLevel,
                           Integer completedSemesters, String admissionType) {

        this.name = name;
        this.department = department;
        this.major = major;
        this.secondaryMajor = secondaryMajor;
        this.isGraduated = isGraduated;
        this.admissionType = admissionType;

        this.academicInfo = AcademicInfo.builder()
                .admissionYear(admissionYear)
                .semesterEnrolled(semesterEnrolled)
                .isTransferStudent(isTransferStudent)
                .status(status)
                .gradeLevel(gradeLevel)
                .completedSemesters(completedSemesters)
                .build();
    }

    public void addStudentCourse(StudentCourse course) {
        this.studentCourses.add(course);
        course.setStudent(this);
    }

    // 연관관계 편의 메서드
    public void setAcademicRecord(StudentAcademicRecord record) {
        this.studentAcademicRecord = record;
        if (record != null) {
            record.setStudent(this);
        }
    }

    public void addSemesterRecord(SemesterAcademicRecord record) {
        this.semesterAcademicRecords.add(record);
        record.setStudent(this);
    }

    public void setTargetGpa(Double targetGpa) {
        this.targetGpa = targetGpa;
    }

    // 학생 정보 업데이트 시 변경 사항 감지 메서드
    public boolean needsUpdate(StudentInitializationDataType newData) {
        if (!equalsNullable(this.name, newData.getName())) return true;
        if (!equalsNullable(this.department, newData.getDepartment())) return true;
        if (!equalsNullable(this.major, newData.getMajor())) return true;
        if (!equalsNullable(this.secondaryMajor, newData.getSecondaryMajor())) return true;
        if (!equalsNullable(this.isGraduated, newData.isGraduated())) return true;
        if (!equalsNullable(this.admissionType, newData.getAdmissionType())) return true;

        AcademicInfo newInfo = AcademicInfo.builder()
                .admissionYear(newData.getAdmissionYear())
                .semesterEnrolled(newData.getSemesterEnrolled())
                .isTransferStudent(newData.isTransferStudent())
                .status(newData.getStatus())
                .gradeLevel(newData.getGradeLevel())
                .completedSemesters(newData.getCompletedSemesters())
                .build();

        return !equalsNullable(this.academicInfo, newInfo);
    }

    private boolean equalsNullable(Object a, Object b) {
        return java.util.Objects.equals(a, b);
    }
}