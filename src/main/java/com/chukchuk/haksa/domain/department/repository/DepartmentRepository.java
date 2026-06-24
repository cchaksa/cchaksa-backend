package com.chukchuk.haksa.domain.department.repository;

import com.chukchuk.haksa.domain.department.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDepartmentCode(String departmentCode);  // departmentCode로 학과 찾기
    List<Department> findAllByEstablishedDepartmentName(String establishedDepartmentName);

    @Query("""
        SELECT d FROM Department d
        WHERE LOWER(d.departmentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(d.establishedDepartmentName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY d.establishedDepartmentName ASC, d.departmentCode ASC
    """)
    List<Department> searchAdminDepartments(String keyword);
}
