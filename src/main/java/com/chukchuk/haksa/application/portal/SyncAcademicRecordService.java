package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.AcademicRecord;
import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollment;
import com.chukchuk.haksa.application.academic.repository.AcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseBulkRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseBulkRow;
import com.chukchuk.haksa.domain.course.dto.CreateOfferingCommand;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.service.CourseOfferingService;
import com.chukchuk.haksa.domain.course.service.CourseService;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.professor.service.ProfessorService;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.infrastructure.portal.mapper.AcademicRecordMapperFromPortal;
import com.chukchuk.haksa.infrastructure.portal.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

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
    private final StudentCourseBulkRepository studentCourseBulkRepository;
    private static final String DEFAULT_PROFESSOR_NAME = "미확인 교수";

    @Transactional
    public SyncAcademicRecordResult executeWithPortalData(UUID userId, PortalData portalData) {
        long t0 = LogTime.start();
        try {
            SyncStats s = sync(userId, portalData, true);
            long tookMs = LogTime.elapsedMs(t0);
            if (tookMs >= SLOW_MS) {
                log.info("[BIZ] sync.done userId={} mode=initial ins={} upd={} del={} took_ms={}",
                        userId, s.inserted, s.updated, s.deleted, tookMs);
            }
            return new SyncAcademicRecordResult(true, null);
        } catch (Exception e) {
            log.error("[BIZ] sync.ex userId={} ex={}", userId, e.getClass().getSimpleName(), e);
            return new SyncAcademicRecordResult(false, "동기화 실패: " + e.getMessage());
        }
    }

    @Transactional
    public SyncAcademicRecordResult executeForRefreshPortalData(UUID userId, PortalData portalData) {
        long t0 = LogTime.start();
        try {
            SyncStats s = sync(userId, portalData, false);
            long tookMs = LogTime.elapsedMs(t0);
            if (tookMs >= SLOW_MS) {
                log.info("[BIZ] sync.done userId={} mode=refresh ins={} upd={} del={} took_ms={}",
                        userId, s.inserted, s.updated, s.deleted, tookMs);
            }
            return new SyncAcademicRecordResult(true, null);
        } catch (Exception e) {
            log.error("[BIZ] sync.ex userId={} ex={}", userId, e.getClass().getSimpleName(), e);
            return new SyncAcademicRecordResult(false, "동기화 실패: " + e.getMessage());
        }
    }

    private SyncStats sync(UUID userId, PortalData portalData, boolean isInitial) {
        long totalStartNs = System.nanoTime();
        Student student = studentService.getStudentByUserId(userId);
        UUID studentId = student.getId();

        long academicStartNs = System.nanoTime();
        AcademicRecord academicRecord = AcademicRecordMapperFromPortal.fromPortalAcademicData(studentId, portalData.academic());
        if (isInitial) {
            academicRecordRepository.insertAllAcademicRecords(academicRecord, student);
        } else {
            academicRecordRepository.updateChangedAcademicRecords(academicRecord, student);
        }
        long academicMs = elapsedMs(academicStartNs);

        // 1) 포털 수강 기록 수집
        CurriculumProcessingResult processingResult =
                processCurriculumData(portalData.curriculum(), portalData.academic(), studentId);
        List<CourseEnrollment> newEnrollments = processingResult.enrollments();
        Map<Long, CourseOffering> offerings = processingResult.offeringById();
        long offeringFetchMs = processingResult.offeringFetchMs();

        // 2) 기존 수강 기록
        List<StudentCourse> existingEnrollments = studentCourseRepository.findByStudent(student);
        Set<Long> existingOfferingIds = existingEnrollments.stream()
                .map(sc -> sc.getOffering().getId())
                .collect(Collectors.toSet());

        // 2-1) 포털 스냅샷 맵 (offeringId -> CourseEnrollment)
        Map<Long, CourseEnrollment> portalEnrollmentMap =
                newEnrollments.stream()
                        .collect(Collectors.toMap(
                                e -> (long) e.getOfferingId(),
                                e -> e,
                                (a, b) -> b
                        ));

        // 2-2) 기존 DB 레코드 갱신 (성적 / 점수 / 재수강 삭제 여부)
        List<StudentCourse> toUpdate = existingEnrollments.stream()
                .map(sc -> {
                    CourseEnrollment pe = portalEnrollmentMap.get(sc.getOffering().getId());
                    if (pe == null || !sc.isDifferentFrom(pe)) return null;
                    sc.updateFromPortal(pe);
                    return sc;
                })
                .filter(Objects::nonNull)
                .toList();

        // 3) 신규 수강 기록 저장 (offeringId 기준 중복 방지)
        long offeringMappingStartNs = System.nanoTime();
        List<StudentCourseBulkRow> newStudentCourses = newEnrollments.stream()
                .filter(e -> !existingOfferingIds.contains((long) e.getOfferingId()))
                .map(e -> {
                    CourseOffering off = offerings.get((long) e.getOfferingId());
                    if (off == null) {
                        log.error("CourseOffering이 존재하지 않는 과목 정보입니다. offeringId={}", e.getOfferingId());
                        return null;
                    }
                    return StudentCourseBulkRow.from(e);
                })
                .filter(Objects::nonNull)
                .toList();
        offeringFetchMs += elapsedMs(offeringMappingStartNs);

        long insertMs = 0L;
        if (!newStudentCourses.isEmpty()) {
            long insertStartNs = System.nanoTime();
            studentCourseBulkRepository.insertAll(newStudentCourses);
            insertMs = elapsedMs(insertStartNs);
        }

        // (삭제) 이전 수강기록 마킹 로직: 포털 값이 진실이므로 더 이상 사용 안 함
        // existingEnrollments.stream() ... markDeletedForRetake()

        // 4) 포털에 없는 offeringId는 제거 (기존 로직 유지)
        long deleteStartNs = System.nanoTime();
        int removed = removeDeletedEnrollments(student, newEnrollments, existingEnrollments);
        long deleteMs = elapsedMs(deleteStartNs);

        SyncStats stats = new SyncStats();
        stats.inserted += newStudentCourses.size();
        stats.updated  += toUpdate.size();
        stats.deleted += removed;

        long totalMs = elapsedMs(totalStartNs);
        log.info("[PERF] portal.sync studentId={} academic_ms={} professor_map_ms={} course_map_ms={} curriculum_merge_ms={} course_get_or_create_ms={} offering_fetch_ms={} insert_ms={} delete_ms={} total_ms={} ins_cnt={} upd_cnt={} del_cnt={}",
                studentId,
                academicMs,
                processingResult.professorMapMs(),
                processingResult.courseMapMs(),
                processingResult.curriculumMergeMs(),
                processingResult.courseGetOrCreateMs(),
                offeringFetchMs,
                insertMs,
                deleteMs,
                totalMs,
                newStudentCourses.size(),
                toUpdate.size(),
                removed);
        return stats;
    }

    private CurriculumProcessingResult processCurriculumData(PortalCurriculumData curriculumData, PortalAcademicData academicData, UUID studentId) {
        long curriculumMergeStartNs = System.nanoTime();
        Map<OfferingKey, MergedOfferingAcademic> mergedOfferings = mergeOfferingsAndAcademic(curriculumData, academicData);
        long curriculumMergeMs = elapsedMs(curriculumMergeStartNs);

        Set<String> professorNames = mergedOfferings.keySet().stream()
                .map(OfferingKey::professorName)
                .collect(Collectors.toSet());
        professorNames.add(DEFAULT_PROFESSOR_NAME);
        long professorLoadStart = System.nanoTime();
        Map<String, Professor> professors = professorService.getOrCreateAll(professorNames);
        long professorMapMs = elapsedMs(professorLoadStart);

        Map<String, String> courseCodeToName = extractCourseNames(curriculumData, mergedOfferings);
        long courseLoadStart = System.nanoTime();
        Map<String, Course> courses = courseService.getOrCreateCourses(courseCodeToName);
        long courseMapMs = elapsedMs(courseLoadStart);

        List<CreateOfferingCommand> offeringCommands = new ArrayList<>();
        Map<OfferingKey, CourseOfferingService.CourseOfferingKey> offeringKeyMap = new HashMap<>();
        for (Map.Entry<OfferingKey, MergedOfferingAcademic> entry : mergedOfferings.entrySet()) {
            OfferingKey key = entry.getKey();
            PortalOfferingCreationData offering = entry.getValue().getOffering();
            Long courseId = Optional.ofNullable(courses.get(offering.getCourseCode()))
                    .map(Course::getId)
                    .orElseThrow(() -> new IllegalStateException("Course not found for code " + offering.getCourseCode()));
            Long professorId = Optional.ofNullable(professors.get(key.professorName()))
                    .map(Professor::getId)
                    .orElseThrow(() -> new IllegalStateException("Professor not found for name " + key.professorName()));

            CreateOfferingCommand command = new CreateOfferingCommand(
                    courseId,
                    offering.getYear(),
                    offering.getSemester(),
                    key.classSection(),
                    professorId,
                    null,
                    offering.getScheduleSummary(),
                    offering.getEvaluationType(),
                    offering.getIsVideoLecture(),
                    offering.getSubjectEstablishmentSemester(),
                    key.facultyDivisionName(),
                    offering.getAreaCode(),
                    offering.getOriginalAreaCode(),
                    offering.getPoints(),
                    key.hostDepartment()
            );
            offeringCommands.add(command);
            offeringKeyMap.put(key, CourseOfferingService.CourseOfferingKey.from(command));
        }

        long offeringLoadStart = System.nanoTime();
        Map<CourseOfferingService.CourseOfferingKey, CourseOffering> offeringEntities =
                courseOfferingService.getOrCreateAll(offeringCommands);
        long courseGetOrCreateMs = elapsedMs(offeringLoadStart);

        long offeringFetchStart = System.nanoTime();
        Map<Long, CourseOffering> offeringById = new HashMap<>();
        List<CourseEnrollment> enrollments = new ArrayList<>();
        for (Map.Entry<OfferingKey, MergedOfferingAcademic> entry : mergedOfferings.entrySet()) {
            CourseOfferingService.CourseOfferingKey serviceKey = offeringKeyMap.get(entry.getKey());
            CourseOffering courseOffering = offeringEntities.get(serviceKey);
            if (courseOffering == null) {
                log.error("CourseOffering not found for key {}", entry.getKey());
                continue;
            }
            PortalOfferingCreationData offering = entry.getValue().getOffering();
            PortalCourseInfo academic = entry.getValue().getAcademic();
            Grade grade = academic != null ? new Grade(GradeType.from(academic.getGrade())) : Grade.createInProgress();
            boolean isRetake = academic != null && academic.isRetake();
            double originalScore = academic != null && academic.getOriginalScore() != null ? academic.getOriginalScore() : 0.0;
            boolean isRetakeDeleted = academic != null && academic.isRetakeDeleted();

            CourseEnrollment enrollment = new CourseEnrollment(
                    studentId,
                    courseOffering.getId(),
                    grade,
                    offering.getPoints(),
                    isRetake,
                    originalScore,
                    isRetakeDeleted
            );
            enrollments.add(enrollment);
            offeringById.put(courseOffering.getId(), courseOffering);
        }
        long offeringFetchMs = elapsedMs(offeringFetchStart);

        return new CurriculumProcessingResult(enrollments, offeringById, professorMapMs, courseMapMs, curriculumMergeMs, courseGetOrCreateMs, offeringFetchMs);
    }

    private Map<OfferingKey, MergedOfferingAcademic> mergeOfferingsAndAcademic(
            PortalCurriculumData curriculumData,
            PortalAcademicData academicData
    ) {
        Map<OfferingKey, MergedOfferingAcademic> map = new HashMap<>();
        Map<SimpleOfferingKey, OfferingKey> simpleKeyIndex = new HashMap<>();

        // 1. offerings 삽입
        if (curriculumData.offerings() != null) {
            for (OfferingInfo offering : curriculumData.offerings()) {
                PortalOfferingCreationData converted = toCreationData(offering);
                OfferingKey key = toOfferingKey(converted);
                map.put(key, new MergedOfferingAcademic(converted, null));
                simpleKeyIndex.put(toSimpleKey(converted), key);
            }
        }

        // 2. academicData로 병합
        if (academicData.semesters() != null) {
            for (SemesterCourseInfo semester : academicData.semesters()) {
                int year = semester.year();
                int semesterNum = semester.semester();

                if (semester.courses() == null) {
                    continue;
                }

                for (CourseInfo course : semester.courses()) {
                    if (course.code() == null) {
                        continue;
                    }
                    SimpleOfferingKey simpleKey = new SimpleOfferingKey(course.code(), year, semesterNum);
                    PortalCourseInfo convertedCourse = toPortalCourseInfo(course);
                    OfferingKey matchedKey = simpleKeyIndex.get(simpleKey);

                    if (matchedKey != null && map.containsKey(matchedKey)) {
                        PortalOfferingCreationData existingOffering = map.get(matchedKey).getOffering();
                        map.put(matchedKey, new MergedOfferingAcademic(existingOffering, convertedCourse));
                        continue;
                    }

                    PortalOfferingCreationData inferredOffering = createInferredOffering(course, year, semesterNum);
                    OfferingKey inferredKey = toOfferingKey(inferredOffering);
                    map.put(inferredKey, new MergedOfferingAcademic(inferredOffering, convertedCourse));
                    simpleKeyIndex.put(simpleKey, inferredKey);
                }
            }
        }

        return map;
    }

    private SimpleOfferingKey toSimpleKey(PortalOfferingCreationData data) {
        return new SimpleOfferingKey(data.getCourseCode(), data.getYear(), data.getSemester());
    }

    private PortalOfferingCreationData createInferredOffering(CourseInfo course, int year, int semesterNum) {
        PortalOfferingCreationData inferredOffering = new PortalOfferingCreationData();
        inferredOffering.setCourseCode(course.code());
        inferredOffering.setYear(year);
        inferredOffering.setSemester(semesterNum);
        inferredOffering.setProfessorName(course.professor());
        inferredOffering.setScheduleSummary(course.schedule());
        inferredOffering.setPoints(course.credits());
        inferredOffering.setSubjectEstablishmentSemester(course.establishmentSemester());
        return inferredOffering;
    }

    int removeDeletedEnrollments(Student student, List<CourseEnrollment> newEnrollments, List<StudentCourse> existingEnrollments) {
        //  새로운 수강 기록의 offeringId 목록 추출
        Set<Long> newOfferingIds = newEnrollments.stream()
                .map(CourseEnrollment::getOfferingId)
                .collect(Collectors.toSet());

        //  기존 수강 기록 중에서 새 목록에 없는 offeringId 탐색
        List<StudentCourse> toRemove = existingEnrollments.stream()
                .filter(sc -> !newOfferingIds.contains(sc.getOffering().getId()))
                .toList();

        //  제거
        if (!toRemove.isEmpty()) {
            List<Long> ids = toRemove.stream()
                    .map(StudentCourse::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (!ids.isEmpty()) {
                studentCourseRepository.deleteAllByIdInBatch(ids);
            }
        }

        return toRemove.size();
    }

    private Map<String, String> extractCourseNames(PortalCurriculumData curriculumData, Map<OfferingKey, MergedOfferingAcademic> mergedOfferings) {
        Map<String, String> courseNames = new HashMap<>();
        if (curriculumData.courses() != null) {
            for (CourseInfo course : curriculumData.courses()) {
                if (course.code() != null) {
                    courseNames.put(course.code(), course.name());
                }
            }
        }
        for (MergedOfferingAcademic offering : mergedOfferings.values()) {
            String code = offering.getOffering().getCourseCode();
            courseNames.putIfAbsent(code, code);
        }
        return courseNames;
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

    private OfferingKey toOfferingKey(PortalOfferingCreationData data) {
        return new OfferingKey(
                data.getCourseCode(),
                data.getYear(),
                data.getSemester(),
                defaultString(data.getClassSection()),
                normalizeProfessorName(data.getProfessorName()),
                defaultString(data.getFacultyDivisionName()),
                defaultString(data.getHostDepartment())
        );
    }

    private String normalizeProfessorName(String name) {
        return (name == null || name.isBlank()) ? DEFAULT_PROFESSOR_NAME : name.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
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
        course.setRetakeDeleted(info.isRetakeDeleted());

        // null-safe 처리
        course.setCredits(info.credits() != null ? info.credits() : 0);
        course.setEstablishmentSemester(info.establishmentSemester() != null ? info.establishmentSemester() : 0);

        return course;
    }

    private List<CourseEnrollment> collapseByCoursePreferLatestNonDeleted(
            List<CourseEnrollment> enrollments, Map<Long, CourseOffering> offerings) {

        // offerings에 없는 id는 스킵 (정상 데이터에서는 아무 것도 걸리지 않음)
        List<CourseEnrollment> safe = enrollments.stream()
                .filter(e -> offerings.containsKey((long) e.getOfferingId()))
                .toList();

        // course_id 단위로 그룹화
        Map<Long, List<CourseEnrollment>> byCourse = safe.stream()
                .collect(Collectors.groupingBy(e ->
                        offerings.get((long) e.getOfferingId()).getCourse().getId()
                ));

        List<CourseEnrollment> result = new ArrayList<>();
        for (List<CourseEnrollment> list : byCourse.values()) {
            // 재수강 '삭제 아님'만 후보
            List<CourseEnrollment> candidates = list.stream()
                    .filter(e -> !e.isRetakeDeleted())
                    .toList();

            // 모두 '재수강 삭제'면 집계 제외
            if (candidates.isEmpty()) continue;

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

    private static class SyncStats { int inserted, updated, deleted; }

    private record OfferingKey(
            String courseCode,
            int year,
            int semester,
            String classSection,
            String professorName,
            String facultyDivisionName,
            String hostDepartment
    ) {}

    private record CurriculumProcessingResult(
            List<CourseEnrollment> enrollments,
            Map<Long, CourseOffering> offeringById,
            long professorMapMs,
            long courseMapMs,
            long curriculumMergeMs,
            long courseGetOrCreateMs,
            long offeringFetchMs
    ) {}

    private record SimpleOfferingKey(
            String courseCode,
            int year,
            int semester
    ) {}

    private long elapsedMs(long startNs) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
    }
}
