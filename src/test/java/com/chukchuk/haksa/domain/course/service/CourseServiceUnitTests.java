package com.chukchuk.haksa.domain.course.service;

import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceUnitTests {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    @DisplayName("기존 과목이 존재하면 해당 과목을 반환한다")
    void getOrCreateCourse_whenExists_returnsExisting() {
        Course existing = new Course("CSE101", "자료구조");
        when(courseRepository.findByCourseCode("CSE101")).thenReturn(Optional.of(existing));

        Course result = courseService.getOrCreateCourse("CSE101", "자료구조");

        assertThat(result).isSameAs(existing);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("기존 과목이 없으면 새 과목을 생성해 저장한다")
    void getOrCreateCourse_whenMissing_createsAndSaves() {
        Course saved = new Course("MAT201", "선형대수");
        when(courseRepository.findByCourseCode("MAT201")).thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class))).thenReturn(saved);

        Course result = courseService.getOrCreateCourse("MAT201", "선형대수");

        assertThat(result).isSameAs(saved);
        verify(courseRepository).save(any(Course.class));
    }
}
