package com.chukchuk.haksa.domain.department.service;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
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
class DepartmentServiceUnitTests {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    @DisplayName("기존 학과가 존재하면 해당 학과를 반환한다")
    void getOrCreateDepartment_whenExists_returnsExisting() {
        Department existing = new Department("CS", "컴퓨터학과");
        when(departmentRepository.findByDepartmentCode("CS")).thenReturn(Optional.of(existing));

        Department result = departmentService.getOrCreateDepartment("CS", "컴퓨터학과");

        assertThat(result).isSameAs(existing);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("기존 학과가 없으면 새 학과를 생성해 저장한다")
    void getOrCreateDepartment_whenMissing_createsAndSaves() {
        Department saved = new Department("EE", "전자공학과");
        when(departmentRepository.findByDepartmentCode("EE")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        Department result = departmentService.getOrCreateDepartment("EE", "전자공학과");

        assertThat(result).isSameAs(saved);
        verify(departmentRepository).save(any(Department.class));
    }
}
