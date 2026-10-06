package com.chukchuk.haksa.infrastructure.portal.mapper;

import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalCourseDto;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalDesignatedCourseDto;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalGradeResponseDto;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalGradeSummaryDto;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalSemesterDto;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalSemesterGradeDto;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalStudentDto;
import com.chukchuk.haksa.infrastructure.portal.model.AcademicSummary;
import com.chukchuk.haksa.infrastructure.portal.model.AdmissionInfo;
import com.chukchuk.haksa.infrastructure.portal.model.CodeName;
import com.chukchuk.haksa.infrastructure.portal.model.CourseInfo;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseSnapshot;
import com.chukchuk.haksa.infrastructure.portal.model.GradeSummary;
import com.chukchuk.haksa.infrastructure.portal.model.OfferingInfo;
import com.chukchuk.haksa.infrastructure.portal.model.PortalAcademicData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalAcademicInfo;
import com.chukchuk.haksa.infrastructure.portal.model.PortalCurriculumData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import com.chukchuk.haksa.infrastructure.portal.model.ProfessorInfo;
import com.chukchuk.haksa.infrastructure.portal.model.Ranking;
import com.chukchuk.haksa.infrastructure.portal.model.SemesterCourseInfo;
import com.chukchuk.haksa.infrastructure.portal.model.SemesterGrade;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/** 워커의 포털 원본 DTO를 학생·학사·교육과정 내부 모델로 변환한다. */
@Slf4j
public class PortalDataMapper {

  private static final Integer DEFAULT_TOTAL_CREDITS = 0;
  private static final Double DEFAULT_GPA = 0.0;
  private static final Pattern AREA_NAME_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*영역\\s*$");

  /**
   * 포털 원본 응답을 내부 포털 데이터로 변환한다.
   *
   * @param raw 학생, 학기별 과목 및 성적 응답을 포함한 워커 payload
   * @return 기본값 보정과 영역·석차 파싱이 적용된 내부 포털 데이터
   * @throws IllegalArgumentException 외국어 인증 값이 지원하는 표현이 아닌 경우
   */
  public static PortalData toPortalData(RawPortalData raw) {
    return new PortalData(
        toPortalStudentInfo(raw.student()),
        toPortalAcademicInfo(raw.semesters(), raw.academicRecords()),
        toPortalCurriculumInfo(raw.semesters()),
        toDesignatedCourseSnapshot(raw.designatedCourses(), raw.student().sno()));
  }

  private static DesignatedCourseSnapshot toDesignatedCourseSnapshot(
      List<RawPortalDesignatedCourseDto> rawCourses, String studentCode) {
    if (rawCourses == null) {
      return DesignatedCourseSnapshot.notReceived();
    }

    List<DesignatedCourseData> courses = new ArrayList<>();
    for (int sourceOrder = 0; sourceOrder < rawCourses.size(); sourceOrder++) {
      RawPortalDesignatedCourseDto raw = rawCourses.get(sourceOrder);
      if (raw == null) {
        throw new IllegalArgumentException("지정과목 행이 null입니다.");
      }
      if (raw.orgClsCd() == null || raw.orgClsCd().isBlank()) {
        throw new IllegalArgumentException("지정과목 orgClsCd가 비어 있습니다.");
      }
      if (raw.subjtCd() == null || raw.subjtCd().isBlank()) {
        throw new IllegalArgumentException("지정과목 subjtCd가 비어 있습니다.");
      }
      if (raw.sno() != null && !raw.sno().isBlank() && !raw.sno().trim().equals(studentCode)) {
        throw new IllegalArgumentException("지정과목 학생번호가 최상위 학생번호와 다릅니다.");
      }
      courses.add(
          new DesignatedCourseData(
              raw.orgClsCd(),
              raw.subjtCd(),
              raw.subjtNm(),
              parseNullableInteger(raw.point(), "point"),
              raw.precpResnCd(),
              parseNullableInteger(raw.cretGainYear(), "cretGainYear"),
              raw.cretSmrNm(),
              raw.sno(),
              sourceOrder));
    }
    return DesignatedCourseSnapshot.received(courses);
  }

  private static PortalStudentInfo toPortalStudentInfo(RawPortalStudentDto s) {
    return new PortalStudentInfo(
        s.sno(),
        s.studNm(),
        new CodeName(s.univCd(), s.univNm()),
        new CodeName(s.dpmjCd(), s.dpmjNm()),
        new CodeName(s.mjorCd(), s.mjorNm()),
        s.the2MjorCd() != null ? new CodeName(s.the2MjorCd(), s.the2MjorNm()) : null,
        s.scrgStatNm(),
        new AdmissionInfo(
            parseIntOrZero(s.enscYear()), parseIntOrZero(s.enscSmrCd()), s.enscDvcd()),
        new PortalAcademicInfo(s.studGrde(), s.facSmrCnt(), DEFAULT_TOTAL_CREDITS, DEFAULT_GPA),
        parseLanguageCertFulfilled(s.flangPassGb()));
  }

  private static PortalAcademicData toPortalAcademicInfo(
      List<RawPortalSemesterDto> semesters, RawPortalGradeResponseDto academicRecords) {
    List<SemesterCourseInfo> semesterCourses = extractSemesterCourses(semesters);
    List<SemesterGrade> grades = extractSemesterGrades(academicRecords);
    AcademicSummary summary = createAcademicSummary(academicRecords);

    return new PortalAcademicData(semesterCourses, new GradeSummary(grades, summary), summary);
  }

  private static List<SemesterCourseInfo> extractSemesterCourses(
      List<RawPortalSemesterDto> semesters) {
    List<SemesterCourseInfo> semesterCourses = new ArrayList<>();

    for (RawPortalSemesterDto sem : semesters) {
      String[] parts = sem.semester().split("-");
      int year = parseIntOrZero(parts[0]);
      int semester = parseIntOrZero(parts[1]);

      List<CourseInfo> courses = new ArrayList<>();
      for (RawPortalCourseDto c : sem.courses()) {
        courses.add(createCourseInfo(c));
      }

      semesterCourses.add(new SemesterCourseInfo(year, semester, courses));
    }

    return semesterCourses;
  }

  private static List<SemesterGrade> extractSemesterGrades(
      RawPortalGradeResponseDto academicRecords) {
    List<SemesterGrade> grades = new ArrayList<>();

    for (RawPortalSemesterGradeDto g : academicRecords.listSmrCretSumTabYearSmr()) {
      log.debug("grade year = {}, semester = '{}'", g.cretGainYear(), g.cretSmrCd());

      grades.add(
          new SemesterGrade(
              parseIntOrZero(g.cretGainYear()),
              parseIntOrZero(g.cretSmrCd()),
              g.gainPoint(),
              g.applPoint(),
              g.gainAvmk(),
              parseDoubleOrZero(g.gainTavgPont()),
              g.dpmjOrdp() != null ? parseRanking(g.dpmjOrdp()) : null));
    }

    return grades;
  }

  private static AcademicSummary createAcademicSummary(RawPortalGradeResponseDto academicRecords) {
    RawPortalGradeSummaryDto summary = academicRecords.selectSmrCretSumTabSjTotal();
    return new AcademicSummary(
        parseIntOrZero(summary.applPoint()),
        parseIntOrZero(summary.gainPoint()),
        parseDoubleOrZero(summary.gainAvmk()),
        parseDoubleOrZero(summary.gainTavgPont()));
  }

  private static CourseInfo createCourseInfo(RawPortalCourseDto c) {
    boolean isRetakeDeleted =
        c.cretDelNm()
            .map(val -> val.trim().equals("재수강 삭제") || val.trim().equals("재수강삭제"))
            .orElse(false);
    Integer credits = c.point() != null ? c.point() : c.gainPoint();
    return new CourseInfo(
        c.subjtCd(),
        c.subjtNm(),
        c.ltrPrfsNm() != null ? c.ltrPrfsNm() : "미확인 교수",
        c.estbDpmjNm(),
        credits,
        c.cretGrdCd(),
        !"-".equals(c.refacYearSmr()), // pick: 기존 is_Retake 처리
        c.timtSmryCn(),
        c.facDvnm(),
        parseIntOrZero(c.cltTerrNm()),
        parseIntOrZero(c.cltTerrCd()),
        parseIntOrZero(c.subjtEstbSmrCd()),
        parseDoubleOrZero(c.gainPont()),
        isRetakeDeleted);
  }

  private static PortalCurriculumData toPortalCurriculumInfo(List<RawPortalSemesterDto> semesters) {
    List<CourseInfo> courses = new ArrayList<>();
    List<ProfessorInfo> professors = new ArrayList<>();
    List<OfferingInfo> offerings = new ArrayList<>();

    for (RawPortalSemesterDto sem : semesters) {
      int year = parseIntOrZero(sem.semester().split("-")[0]);
      int semester = parseIntOrZero(sem.semester().split("-")[1]);

      for (RawPortalCourseDto c : sem.courses()) {
        courses.add(createCourseInfo(c));
        professors.add(new ProfessorInfo(c.ltrPrfsNm() != null ? c.ltrPrfsNm() : "미확인 교수"));

        offerings.add(
            new OfferingInfo(
                c.subjtCd(),
                year,
                semester,
                c.diclNo(),
                c.ltrPrfsNm(),
                c.timtSmryCn(),
                c.point() != null ? c.point() : c.gainPoint(),
                c.estbDpmjNm(),
                c.facDvnm(),
                parseIntOrZero(c.subjtEstbSmrCd()),
                extractAreaCode(c.cltTerrNm()),
                parseIntOrZero(c.cltTerrCd()),
                "UNKNOWN",
                false));
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

  private static Integer parseNullableInteger(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("지정과목 " + fieldName + " 값이 숫자가 아닙니다: " + value, exception);
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

  private static Boolean parseLanguageCertFulfilled(String flangPassGb) {
    if (flangPassGb == null || flangPassGb.isBlank()) {
      return null;
    }

    String normalized = flangPassGb.trim();
    if ("통과".equals(normalized)) {
      return true;
    }
    if ("미통과".equals(normalized)) {
      return false;
    }

    throw new IllegalArgumentException("알 수 없는 외국어 인증 값: " + flangPassGb);
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

  private static int extractAreaCode(String input) {
    if (input == null) {
      return 0;
    }
    Matcher matcher = AREA_NAME_PATTERN.matcher(input);
    if (!matcher.matches()) {
      return 0;
    }
    try {
      return Integer.parseInt(matcher.group(1));
    } catch (NumberFormatException exception) {
      return 0;
    }
  }
}
