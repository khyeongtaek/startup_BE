package org.goodee.startup_BE.work_log.repository;

import org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO;
import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
	@Query(value = """
    SELECT new org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO(
        w.workLogId,
        w.employee.name,
        w.workType.value2,
        w.workOption.value2,
        w.workDate,
        w.title,
        w.content,
        CASE WHEN r.readId IS NOT NULL THEN true ELSE false END
    )
    FROM WorkLog w
    LEFT JOIN WorkLogRead r
      ON r.workLog = w
     AND r.employee.employeeId = :empId
    WHERE (:deptId IS NULL OR w.employee.department.commonCodeId = :deptId)
      AND (:onlyMine = false OR w.employee.employeeId = :empId)
    """,
		countQuery = """
      SELECT COUNT(w)
      FROM WorkLog w
      WHERE (:deptId IS NULL OR w.employee.department.commonCodeId = :deptId)
        AND (:onlyMine = false OR w.employee.employeeId = :empId)
    """)
	Page<WorkLogResponseDTO> findWithRead(
		@Param("empId") Long empId,
		@Param("deptId") Long deptId,         // 전체/내: null, 부서: 부서 CommonCode PK
		@Param("onlyMine") boolean onlyMine,  // 내 것: true, 나머지: false
		Pageable pageable
	);
	
	
}
