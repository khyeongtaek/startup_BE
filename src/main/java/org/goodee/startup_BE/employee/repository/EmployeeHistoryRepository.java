package org.goodee.startup_BE.employee.repository;

import org.goodee.startup_BE.employee.entity.EmployeeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeHistoryRepository extends JpaRepository<EmployeeHistory, Long> {

    Page<EmployeeHistory> findByEmployeeEmployeeId(Long employeeId, Pageable pageable);

}
