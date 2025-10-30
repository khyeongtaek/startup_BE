package org.goodee.startup_BE.work_log.repository;


import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.goodee.startup_BE.work_log.entity.WorkLogRead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkLogReadRepository extends JpaRepository<WorkLogRead, Long> {
	boolean existsByWorkLogAndEmployee(WorkLog workLog, Employee employee);
}