package com.chukchuk.haksa.domain.course.service;

import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;

    @Transactional
    public Course getOrCreateCourse(String courseCode, String courseName) {
        return courseRepository.findByCourseCode(courseCode)
                .orElseGet(() -> {
                    Course newCourse = new Course(courseCode, courseName);
                    return courseRepository.save(newCourse);
                });
    }

    @Transactional
    public Map<String, Course> getOrCreateCourses(Map<String, String> courseCodeToName) {
        if (courseCodeToName == null || courseCodeToName.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> codes = courseCodeToName.keySet().stream()
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Course> existing = courseRepository.findByCourseCodeIn(codes);
        Map<String, Course> result = new HashMap<>();
        for (Course course : existing) {
            result.put(course.getCourseCode(), course);
        }

        List<Course> toCreate = codes.stream()
                .filter(code -> !result.containsKey(code))
                .map(code -> new Course(code, courseCodeToName.get(code)))
                .toList();

        if (!toCreate.isEmpty()) {
            List<Course> saved = courseRepository.saveAll(toCreate);
            for (Course course : saved) {
                result.put(course.getCourseCode(), course);
            }
        }

        return result;
    }
}
