package com.chukchuk.haksa.domain.course.service;

import com.chukchuk.haksa.domain.course.dto.CreateOfferingCommand;
import com.chukchuk.haksa.domain.course.model.*;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import com.chukchuk.haksa.domain.course.repository.LiberalArtsAreaCodeRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.professor.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseRepository courseRepository;
    private final LiberalArtsAreaCodeRepository liberalArtsAreaCodeRepository;
    private final ProfessorRepository professorRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public CourseOffering getOrCreateOffering(CreateOfferingCommand cmd) {
        return getOrCreateAll(List.of(cmd)).get(CourseOfferingKey.from(cmd));
    }

    @Transactional
    public Map<CourseOfferingKey, CourseOffering> getOrCreateAll(List<CreateOfferingCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return Map.of();
        }

        Map<CourseOfferingKey, CreateOfferingCommand> commandByKey = new HashMap<>();
        for (CreateOfferingCommand command : commands) {
            commandByKey.put(CourseOfferingKey.from(command), command);
        }

        Set<Long> courseIds = commandByKey.keySet().stream().map(CourseOfferingKey::courseId).collect(Collectors.toSet());
        Set<Integer> years = commandByKey.keySet().stream().map(CourseOfferingKey::year).collect(Collectors.toSet());
        Set<Integer> semesters = commandByKey.keySet().stream().map(CourseOfferingKey::semester).collect(Collectors.toSet());

        List<CourseOffering> existing = courseOfferingRepository.findByCourseIdInAndYearInAndSemesterIn(courseIds, years, semesters);
        Map<CourseOfferingKey, CourseOffering> result = new HashMap<>();
        for (CourseOffering offering : existing) {
            CourseOfferingKey key = CourseOfferingKey.from(offering);
            if (commandByKey.containsKey(key)) {
                result.putIfAbsent(key, offering);
            }
        }

        List<CourseOffering> toInsert = new ArrayList<>();
        for (Map.Entry<CourseOfferingKey, CreateOfferingCommand> entry : commandByKey.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                CourseOffering created = createCourseOffering(entry.getValue());
                toInsert.add(created);
                result.put(entry.getKey(), created);
            }
        }

        if (!toInsert.isEmpty()) {
            courseOfferingRepository.saveAll(toInsert);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, CourseOffering> getOfferingMapByIds(List<Long> offeringIds) {
        return courseOfferingRepository.findAllById(offeringIds).stream()
                .collect(Collectors.toMap(CourseOffering::getId, o -> o));
    }

    private CourseOffering createCourseOffering(CreateOfferingCommand cmd) {
        Course course = courseRepository.getReferenceById(cmd.courseId());
        Professor professor = professorRepository.getReferenceById(cmd.professorId());
        Department department = cmd.departmentId() != null
                ? departmentRepository.getReferenceById(cmd.departmentId())
                : null;

        LiberalArtsAreaCode liberalArtsAreaCode = null;

        if (cmd.areaCode() != null && cmd.areaCode() != 0) {
            liberalArtsAreaCode = liberalArtsAreaCodeRepository.getReferenceById(cmd.areaCode());
        }

        return new CourseOffering(
                cmd.subjectEstablishmentSemester(),
                cmd.isVideoLecture(),
                cmd.year(),
                cmd.semester(),
                cmd.hostDepartment(),
                cmd.classSection(),
                cmd.scheduleSummary(),
                cmd.originalAreaCode(),
                cmd.points(),
                EvaluationType.valueOf(cmd.evaluationType()),
                FacultyDivision.valueOf(cmd.facultyDivisionName()),
                course,
                professor,
                department,
                liberalArtsAreaCode
        );
    }

    public record CourseOfferingKey(
            Long courseId,
            Integer year,
            Integer semester,
            String classSection,
            Long professorId,
            String facultyDivisionName,
            String hostDepartment
    ) {
        public static CourseOfferingKey from(CreateOfferingCommand cmd) {
            return new CourseOfferingKey(
                    cmd.courseId(),
                    cmd.year(),
                    cmd.semester(),
                    cmd.classSection(),
                    cmd.professorId(),
                    cmd.facultyDivisionName(),
                    cmd.hostDepartment()
            );
        }

        public static CourseOfferingKey from(CourseOffering offering) {
            return new CourseOfferingKey(
                    offering.getCourse().getId(),
                    offering.getYear(),
                    offering.getSemester(),
                    offering.getClassSection(),
                    offering.getProfessor().getId(),
                    offering.getFacultyDivisionName().name(),
                    offering.getHostDepartment()
            );
        }
    }
}
