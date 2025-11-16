package org.goodee.startup_BE.employee.repository;

import org.goodee.startup_BE.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);

    Boolean existsByEmail(String email);

    Boolean existsByUsername(String username);

    // 부서(CommonCode)로 직원 목록을 조회, 직급(position)의 정렬순서(sortOrder)를 내림차순(Desc)으로 정렬
    List<Employee> findByDepartmentCommonCodeIdOrderByPositionSortOrderDesc(Long departmentId);

    // 메일 - 이메일로 수신자 조회
    List<Employee> findAllByEmailIn(Collection<String> emails);
}
