package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.AcademicRecord;
import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollment;
import com.chukchuk.haksa.application.academic.repository.AcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.course.dto.CreateOfferingCommand;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.service.CourseOfferingService;
import com.chukchuk.haksa.domain.course.service.CourseService;
import com.chukchuk.haksa.domain.professor.service.ProfessorService;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.infrastructure.portal.mapper.AcademicRecordMapperFromPortal;
import com.chukchuk.haksa.infrastructure.portal.mapper.StudentCourseMapper;
import com.chukchuk.haksa.infrastructure.portal.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/* 학업 이력 동기화 유스케이스 실행 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncAcademicRecordService {

    private final AcademicRecordRepository academicRecordRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final StudentService studentService;
    private final CourseOfferingService courseOfferingService;
    private final ProfessorService professorService;
    private final CourseService courseService;

    @Transactional
    public SyncAcademicRecordResult executeWithPortalData(UUID userId, PortalData portalData) {
        try {
            sync(userId, portalData, true);
            return new SyncAcademicRecordResult(true, null);
        } catch (Exception e) {
            log.error("학업 이력 동기화 중 오류 발생", e);
            return new SyncAcademicRecordResult(false, "동기화 실패: " + e.getMessage());
        }
    }

    @Transactional
    public SyncAcademicRecordResult executeForRefreshPortalData(UUID userId, PortalData portalData) {
        try {
            sync(userId, portalData, false);
            return new SyncAcademicRecordResult(true, null);
        } catch (Exception e) {
            log.error("학업 이력 동기화 중 오류 발생", e);
            return new SyncAcademicRecordResult(false, "동기화 실패: " + e.getMessage());
        }
    }

    private void sync(UUID userId, PortalData portalData, boolean isInitial) {
        Student student = studentService.getStudentByUserId(userId);
        UUID studentId = student.getId();

        AcademicRecord academicRecord = AcademicRecordMapperFromPortal.fromPortalAcademicData(studentId, portalData.academic());
        if (isInitial) {
            academicRecordRepository.insertAllAcademicRecords(academicRecord, student);
        } else {
            academicRecordRepository.updateChangedAcademicRecords(academicRecord, student);
        }

        List<CourseEnrollment> enrollments = processCurriculumData(portalData.curriculum(), portalData.academic(), studentId);

        List<Long> offeringIds = enrollments.stream()
                .map(e -> (long) e.getOfferingId())
                .distinct()
                .toList();

        Map<Long, CourseOffering> offerings = courseOfferingService.getOfferingMapByIds(offeringIds);

        // 동일 course_id에 대해 과목당 1건만 남기기 (재수강 우선, 최신 연/학기 우선)
        List<CourseEnrollment> collapsed = collapseByCoursePreferLatestRetake(enrollments, offerings);

        List<StudentCourse> existingEnrollments = studentCourseRepository.findByStudent(student);

        Set<Long> existingOfferingIds = existingEnrollments.stream()
                .map(sc -> sc.getOffering().getId())
                .collect(Collectors.toSet());

        // 1) offeringId -> courseId 매핑
        Map<Long, Long> offeringToCourseId = offerings.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        CourseOffering::getId,
                        off -> off.getCourse().getId()
                ));

        // 2) 재수강 enrollment 모으기 (collapse 결과 기준)
        List<CourseEnrollment> retakeEnrollments = collapsed.stream()
                .filter(CourseEnrollment::isRetake)
                .toList();

        // 3) 재수강 과목의 courseId 집합
        Set<Long> retakeCourseIds = retakeEnrollments.stream()
                .map(e -> offeringToCourseId.get((long) e.getOfferingId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4) 기존 기록 중 재수강 대상 과목 삭제 마킹
        List<StudentCourse> toRemoveForRetake = existingEnrollments.stream()
                .filter(sc -> retakeCourseIds.contains(sc.getOffering().getCourse().getId()))
                .toList();

        if (!toRemoveForRetake.isEmpty()) {
            toRemoveForRetake.forEach(sc -> {
                sc.markDeletedForRetake(); // 플래그만 변경
                existingOfferingIds.remove(sc.getOffering().getId());
            });
            studentCourseRepository.saveAll(toRemoveForRetake);
        }

        // 5-1) 재수강 아닌 과목도 저장하되 deletedForRetake = true로 설정
        Set<Long> collapsedOfferingIds = collapsed.stream()
                .map(e -> (long) e.getOfferingId())
                .collect(Collectors.toSet());

        List<CourseEnrollment> deletedForRetakeEnrollments = enrollments.stream()
                .filter(e -> !collapsedOfferingIds.contains((long) e.getOfferingId()))
                .filter(e -> !existingOfferingIds.contains((long) e.getOfferingId()))
                .toList();

        List<StudentCourse> deletedForRetakeCourses = deletedForRetakeEnrollments.stream()
                .map(e -> {
                    CourseOffering off = offerings.get((long) e.getOfferingId());
                    if (off == null) return null;

                    StudentCourse course = StudentCourseMapper.toEntity(e, student, off);
                    course.markDeletedForRetake(); // 재수강 이전 과목 마킹
                    return course;
                })
                .filter(Objects::nonNull)
                .toList();

        // 5-2) 재수강 최종 과목만 deletedForRetake = false로 저장
        List<StudentCourse> newStudentCourses = collapsed.stream()
                .filter(e -> !existingOfferingIds.contains((long) e.getOfferingId()))
                .map(e -> {
                    CourseOffering off = offerings.get((long) e.getOfferingId());
                    if (off == null) return null;

                    return StudentCourseMapper.toEntity(e, student, off); // 기본값 false
                })
                .filter(Objects::nonNull)
                .toList();

        newStudentCourses.forEach(student::addStudentCourse);
        deletedForRetakeCourses.forEach(student::addStudentCourse);

        studentCourseRepository.saveAll(deletedForRetakeCourses);
        studentCourseRepository.saveAll(newStudentCourses);

        removeDeletedEnrollments(student, collapsed);
    }

    /* offerings(교과)와 academic(학업 성적)을 합쳐서
     *  최종적으로 CourseEnrollment를 만드는 메서드
     *  */
    private List<CourseEnrollment> processCurriculumData(PortalCurriculumData curriculumData, PortalAcademicData academicData, UUID studentId) {
        List<CourseEnrollment> enrollments = new ArrayList<>();
        Map<String, MergedOfferingAcademic> mergedList = mergeOfferingsAndAcademic(curriculumData, academicData);

        // 교수/과목 정보 미리 조회
        Map<String, Long> professorMap = buildProfessorMap(curriculumData);
        Map<String, Long> courseMap = buildCourseMap(curriculumData);

        for (MergedOfferingAcademic item : mergedList.values()) {
            PortalOfferingCreationData offering = item.getOffering();
            PortalCourseInfo academic = item.getAcademic();

            // 과목 ID 및 교수 ID 구하기
            Long courseId = courseMap.get(offering.getCourseCode());
            String professorName = offering.getProfessorName() != null ? offering.getProfessorName() : "미확인 교수";
            Long professorId = professorMap.get(professorName);

            CreateOfferingCommand createOfferingCommand = new CreateOfferingCommand(
                    courseId,
                    offering.getYear(),
                    offering.getSemester(),
                    offering.getClassSection(),
                    professorId,
                    null, // departmentId → 확장 가능
                    offering.getScheduleSummary(),
                    offering.getEvaluationType(),
                    offering.getIsVideoLecture(),
                    offering.getSubjectEstablishmentSemester(),
                    offering.getFacultyDivisionName(),
                    offering.getAreaCode(),
                    offering.getOriginalAreaCode(),
                    offering.getPoints(),
                    offering.getHostDepartment()
            );

            // 개설강좌 및 성적 처리
            CourseOffering courseOffering = courseOfferingService.getOrCreateOffering(createOfferingCommand);
            Grade grade = academic != null
                    ? new Grade(GradeType.from(academic.getGrade()))
                    : Grade.createInProgress();

            boolean isRetake = academic != null && academic.isRetake();
            double originalScore = Optional.ofNullable(academic.getOriginalScore()).orElse(0.0);

            CourseEnrollment enrollment = new CourseEnrollment(studentId, courseOffering.getId(), grade, offering.getPoints(), isRetake, originalScore);
            enrollments.add(enrollment);
        }

        return enrollments;
    }

    private Map<String, MergedOfferingAcademic> mergeOfferingsAndAcademic(
            PortalCurriculumData curriculumData,
            PortalAcademicData academicData
    ) {
        Map<String, MergedOfferingAcademic> map = new HashMap<>();

        // 1. offerings 삽입
        for (OfferingInfo offering : curriculumData.offerings()) {
            String key = makeKey(offering.year(), offering.semester(), offering.courseCode());
            PortalOfferingCreationData converted = toCreationData(offering);
            map.put(key, new MergedOfferingAcademic(converted, null));
        }

        // 2. academicData로 병합
        for (SemesterCourseInfo semester : academicData.semesters()) {
            int year = semester.year();
            int semesterNum = semester.semester();

            for (CourseInfo course : semester.courses()) {
                String key = makeKey(year, semesterNum, course.code());

                PortalCourseInfo convertedCourse = toPortalCourseInfo(course);

                if (map.containsKey(key)) {
                    // 이미 존재하는 offering에 academic만 추가
                    PortalOfferingCreationData existingOffering = map.get(key).getOffering();
                    map.put(key, new MergedOfferingAcademic(existingOffering, convertedCourse));
                } else {
                    // academic만 존재하는 경우 → offering은 기본값 생성
                    PortalOfferingCreationData inferredOffering = new PortalOfferingCreationData();
                    inferredOffering.setCourseCode(course.code());
                    inferredOffering.setYear(year);
                    inferredOffering.setSemester(semesterNum);
                    inferredOffering.setProfessorName(course.professor());
                    inferredOffering.setScheduleSummary(course.schedule());
                    inferredOffering.setPoints(course.credits());
                    inferredOffering.setSubjectEstablishmentSemester(course.establishmentSemester());
                    // 다른 필드는 null로 유지

                    map.put(key, new MergedOfferingAcademic(inferredOffering, convertedCourse));
                }
            }
        }

        return map;
    }

    private void removeDeletedEnrollments(Student student, List<CourseEnrollment> newEnrollments) {
        // 1. DB에서 해당 학생의 기존 수강 기록 전체 조회
        List<StudentCourse> existingEnrollments = studentCourseRepository.findByStudent(student);

        // 2. 새로운 수강 기록의 offeringId 목록 추출
        Set<Long> newOfferingIds = newEnrollments.stream()
                .map(CourseEnrollment::getOfferingId)
                .collect(Collectors.toSet());

        // 3. 기존 수강 기록 중에서 새 목록에 없는 offeringId 탐색
        List<StudentCourse> toRemove = existingEnrollments.stream()
                .filter(sc -> !newOfferingIds.contains(sc.getOffering().getId()))
                .toList();

        // 4. 제거
        if (!toRemove.isEmpty()) {
            studentCourseRepository.deleteAll(toRemove);
        }
    }

    private Map<String, Long> buildProfessorMap(PortalCurriculumData curriculumData) {
        Map<String, Long> professorMap = new HashMap<>();

        for (ProfessorInfo prof : curriculumData.professors()) {
            String name = prof.professorName() != null ? prof.professorName() : "미확인 교수";

            // DB에서 getOrCreate
            Long id = professorService.getOrCreate(name).getId();
            professorMap.put(name, id);
        }

        // 명시적으로 "미확인 교수"도 포함
        if (!professorMap.containsKey("미확인 교수")) {
            Long id = professorService.getOrCreate("미확인 교수").getId();
            professorMap.put("미확인 교수", id);
        }


        return professorMap;
    }

    private Map<String, Long> buildCourseMap(PortalCurriculumData curriculumData) {
        Map<String, Long> courseMap = new HashMap<>();

        for (CourseInfo course : curriculumData.courses()) {
            String courseCode = course.code();

            // DB에서 getOrCreate
            Long id = courseService.getOrCreateCourse(
                    courseCode,
                    course.name()
            ).getId();

            courseMap.put(courseCode, id);
        }

        return courseMap;
    }

    private String makeKey(int year, int semester, String courseCode) {
        return year + "-" + semester + "-" + courseCode;
    }

    private PortalOfferingCreationData toCreationData(OfferingInfo info) {
        log.debug("from OfferingInfo, facultyDivisionName: {}, evaluationType:{}", info.facultyDivisionName(), info.evaluationType());

        PortalOfferingCreationData data = new PortalOfferingCreationData();
        data.setCourseCode(info.courseCode());
        data.setYear(info.year());
        data.setSemester(info.semester());
        data.setClassSection(info.classSection());
        data.setProfessorName(info.professorName());
        data.setScheduleSummary(info.scheduleSummary());
        data.setPoints(info.points());
        data.setHostDepartment(info.hostDepartment());
        data.setFacultyDivisionName(info.facultyDivisionName());
        data.setSubjectEstablishmentSemester(info.subjectEstablishmentSemester());
        data.setAreaCode(info.areaCode());
        data.setOriginalAreaCode(info.originalAreaCode());

        // 누락된 필드 추가
        data.setEvaluationType(info.evaluationType());
        data.setIsVideoLecture(info.isVideoLecture());

        return data;
    }

    private PortalCourseInfo toPortalCourseInfo(CourseInfo info) {
        PortalCourseInfo course = new PortalCourseInfo();

        course.setCode(info.code());
        course.setName(info.name());
        course.setProfessor(info.professor());
        course.setSchedule(info.schedule());
        course.setGrade(info.grade());
        course.setRetake(info.isRetake());
        course.setOriginalScore(info.originalScore());

        // null-safe 처리
        course.setCredits(info.credits() != null ? info.credits() : 0);
        course.setEstablishmentSemester(info.establishmentSemester() != null ? info.establishmentSemester() : 0);

        return course;
    }

    private List<CourseEnrollment> collapseByCoursePreferLatestRetake(
            List<CourseEnrollment> enrollments, Map<Long, CourseOffering> offerings) {

        // offerings에 없는 id는 스킵 (정상 데이터에서는 아무 것도 걸리지 않음)
        List<CourseEnrollment> safe = enrollments.stream()
                .filter(e -> offerings.containsKey((long) e.getOfferingId()))
                .toList();

        Map<Long, List<CourseEnrollment>> byCourse = safe.stream()
                .collect(Collectors.groupingBy(e ->
                        offerings.get((long) e.getOfferingId()).getCourse().getId()
                ));

        List<CourseEnrollment> result = new ArrayList<>();
        for (List<CourseEnrollment> list : byCourse.values()) {
            List<CourseEnrollment> retakes = list.stream().filter(CourseEnrollment::isRetake).toList();
            List<CourseEnrollment> candidates = retakes.isEmpty() ? list : retakes;

            // 최신(연도→학기)만 선택
            CourseEnrollment picked = candidates.stream()
                    .max(Comparator
                            .comparingInt((CourseEnrollment e) -> offerings.get((long) e.getOfferingId()).getYear())
                            .thenComparingInt(e -> offerings.get((long) e.getOfferingId()).getSemester())
                    )
                    .orElse(null);

            if (picked != null) result.add(picked);
        }
        return result;
    }
}