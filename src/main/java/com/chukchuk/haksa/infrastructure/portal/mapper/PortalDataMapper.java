package com.chukchuk.haksa.infrastructure.portal.mapper;

import com.chukchuk.haksa.infrastructure.portal.dto.raw.*;
import com.chukchuk.haksa.infrastructure.portal.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PortalDataMapper {

    private static final Integer DEFAULT_TOTAL_CREDITS = 0;
    private static final Double DEFAULT_GPA = 0.0;

    public static PortalData toPortalData(RawPortalData raw) {
        return new PortalData(
                toPortalStudentInfo(raw.student()),
                toPortalAcademicInfo(raw.semesters(), raw.academicRecords()),
                toPortalCurriculumInfo(raw.semesters())
        );
    }

    private static PortalStudentInfo toPortalStudentInfo(RawPortalStudentDTO s) {
        return new PortalStudentInfo(
                s.sno(),
                s.studNm(),
                new CodeName(s.univCd(), s.univNm()),
                new CodeName(s.dpmjCd(), s.dpmjNm()),
                new CodeName(s.mjorCd(), s.mjorNm()),
                s.the2MjorCd() != null ? new CodeName(s.the2MjorCd(), s.the2MjorNm()) : null,
                s.scrgStatNm(),
                new AdmissionInfo(
                        parseIntOrZero(s.enscYear()),
                        parseIntOrZero(s.enscSmrCd()),
                        s.enscDvcd()
                ),
                new PortalAcademicInfo(
                        s.studGrde(),
                        s.facSmrCnt(),
                        DEFAULT_TOTAL_CREDITS,
                        DEFAULT_GPA
                )
        );
    }

    private static PortalAcademicData toPortalAcademicInfo(List<RawPortalSemesterDTO> semesters, RawPortalGradeResponseDTO academicRecords) {
        List<SemesterCourseInfo> semesterCourses = extractSemesterCourses(semesters);
        List<SemesterGrade> grades = extractSemesterGrades(academicRecords);
        AcademicSummary summary = createAcademicSummary(academicRecords);

        return new PortalAcademicData(semesterCourses, new GradeSummary(grades, summary), summary);
    }

    private static List<SemesterCourseInfo> extractSemesterCourses(List<RawPortalSemesterDTO> semesters) {
        List<SemesterCourseInfo> semesterCourses = new ArrayList<>();

        for (RawPortalSemesterDTO sem : semesters) {
            String[] parts = sem.semester().split("-");
            int year = parseIntOrZero(parts[0]);
            int semester = parseIntOrZero(parts[1]);

            List<CourseInfo> courses = new ArrayList<>();
            for (RawPortalCourseDTO c : sem.courses()) {
                courses.add(createCourseInfo(c));
            }

            semesterCourses.add(new SemesterCourseInfo(year, semester, courses));
        }

        return semesterCourses;
    }

    private static List<SemesterGrade> extractSemesterGrades(RawPortalGradeResponseDTO academicRecords) {
        List<SemesterGrade> grades = new ArrayList<>();

        for (RawPortalSemesterGradeDTO g : academicRecords.listSmrCretSumTabYearSmr()) {
            log.debug("grade year = {}, semester = '{}'", g.cretGainYear(), g.cretSmrCd());

            grades.add(new SemesterGrade(
                    parseIntOrZero(g.cretGainYear()),
                    parseIntOrZero(g.cretSmrCd()),
                    g.gainPoint(),
                    g.applPoint(),
                    g.gainAvmk(),
                    parseDoubleOrZero(g.gainTavgPont()),
                    g.dpmjOrdp() != null ? parseRanking(g.dpmjOrdp()) : null
            ));
        }

        return grades;
    }

    private static AcademicSummary createAcademicSummary(RawPortalGradeResponseDTO academicRecords) {
        RawPortalGradeSummaryDTO summary = academicRecords.selectSmrCretSumTabSjTotal();
        return new AcademicSummary(
                parseIntOrZero(summary.applPoint()),
                parseIntOrZero(summary.gainPoint()),
                parseDoubleOrZero(summary.gainAvmk()),
                parseDoubleOrZero(summary.gainTavgPont())
        );
    }

    private static CourseInfo createCourseInfo(RawPortalCourseDTO c) {
        boolean isRetakeDeleted = c.cretDelNm()
                .map(val -> val.trim().equals("재수강 삭제") || val.trim().equals("재수강삭제"))
                .orElse(false);
        return new CourseInfo(
                c.subjtCd(),
                c.subjtNm(),
                c.ltrPrfsNm() != null ? c.ltrPrfsNm() : "미확인 교수",
                c.estbDpmjNm(),
                c.point(),
                c.cretGrdCd(),
                !"-".equals(c.refacYearSmr()), // pick: 기존 is_Retake 처리
                c.timtSmryCn(),
                c.facDvnm(),
                parseIntOrZero(c.cltTerrNm()),
                parseIntOrZero(c.cltTerrCd()),
                parseIntOrZero(c.subjtEstbSmrCd()),
                parseDoubleOrZero(c.gainPont()),
                isRetakeDeleted
        );
    }

    private static PortalCurriculumData toPortalCurriculumInfo(List<RawPortalSemesterDTO> semesters) {
        List<CourseInfo> courses = new ArrayList<>();
        List<ProfessorInfo> professors = new ArrayList<>();
        List<OfferingInfo> offerings = new ArrayList<>();

        for (RawPortalSemesterDTO sem : semesters) {
            int year = parseIntOrZero(sem.semester().split("-")[0]);
            int semester = parseIntOrZero(sem.semester().split("-")[1]);

            for (RawPortalCourseDTO c : sem.courses()) {
                courses.add(createCourseInfo(c));
                professors.add(new ProfessorInfo(c.ltrPrfsNm() != null ? c.ltrPrfsNm() : "미확인 교수"));

                offerings.add(new OfferingInfo(
                        c.subjtCd(),
                        year,
                        semester,
                        c.diclNo(),
                        c.ltrPrfsNm(),
                        c.timtSmryCn(),
                        c.point(),
                        c.estbDpmjNm(),
                        c.facDvnm(),
                        parseIntOrZero(c.subjtEstbSmrCd()),
                        extractLeadingDigit(c.cltTerrNm()), // cltTerrNm: 선교 영역 (ex. '6영역'처럼 데이터가 넘어옴) -> 영역 숫자만 추출
                        parseIntOrZero(c.cltTerrCd()),
                        "UNKNOWN", // TODO: 변경 필요 시 필드 추가
                        false
                ));
            }
        }

        return new PortalCurriculumData(courses, professors, offerings);
    }

    // 기본값 0 반환
    private static int parseIntOrZero(String str) {
        try {
            return (str != null && !str.isBlank()) ? Integer.parseInt(str) : 0;
        } catch (NumberFormatException e) {
            log.debug("parseInt 실패: {}", str);
            return 0;
        }
    }

    private static double parseDoubleOrZero(String str) {
        try {
            return (str != null && !str.isBlank()) ? Double.parseDouble(str) : 0.0;
        } catch (NumberFormatException e) {
            log.debug("parseDouble 실패: {}", str);
            return 0.0;
        }
    }

    private static Ranking parseRanking(String ordp) {
        if (ordp == null || ordp.isBlank()) {
            log.debug("parseRanking: 입력이 null이거나 공백입니다. → '{}'", ordp);
            return null;
        }

        String[] split = ordp.split("/");
        if (split.length != 2 || split[0].isBlank() || split[1].isBlank()) {
            log.debug("parseRanking: 올바르지 않은 포맷입니다. → '{}'", ordp);
            return null;
        }

        try {
            int rank = Integer.parseInt(split[0].trim());
            int total = Integer.parseInt(split[1].trim());
            return new Ranking(rank, total);
        } catch (NumberFormatException e) {
            log.debug("parseRanking: 숫자 파싱 실패 → '{}'", ordp);
            return null;
        }
    }

    private static int extractLeadingDigit(String input) {
        if (input != null && !input.isBlank() && Character.isDigit(input.charAt(0))) {
            return Character.getNumericValue(input.charAt(0));
        }
        return 0;
    }
}