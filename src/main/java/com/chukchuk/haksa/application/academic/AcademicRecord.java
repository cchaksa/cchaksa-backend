package com.chukchuk.haksa.application.academic;


import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AcademicRecord {
    private UUID studentId;
    private List<SemesterGrade> semesters;
    private AcademicSummary summary;

    public AcademicRecord(UUID studentId, List<SemesterGrade> semesters, AcademicSummary summary) {
        this.studentId = studentId;
        this.semesters = semesters;
        this.summary = summary;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public List<SemesterGrade> getSemesters() {
        if (semesters == null) {
            return Collections.emptyList();
        }
        Collections.sort(semesters, (a, b) -> {
            if (b.getYear() != a.getYear()) {
                return Integer.compare(b.getYear(), a.getYear());
            }
            return Integer.compare(b.getSemester(), a.getSemester());
        });
        return semesters;
    }

    public AcademicSummary getSummary() {
        return summary;
    }
}
