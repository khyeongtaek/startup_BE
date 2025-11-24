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
        CASE WHEN e IS NULL THEN '정보 없음' ELSE e.name END,
        e.profileImg,
        e.department.value1,
        w.workType.value2,
        w.workOption.value2,
        w.workDate,
        w.title,
        w.content,
        w.workType.commonCodeId,
        w.workOption.commonCodeId,
        CASE WHEN r.readId IS NOT NULL THEN true ELSE false END
    )
    FROM WorkLog w
    LEFT JOIN w.employee e
    LEFT JOIN WorkLogRead r
      ON r.workLog = w
     AND r.employee.employeeId = :empId
    WHERE (
           :deptId IS NULL
           OR (e IS NOT NULL AND e.department.commonCodeId = :deptId)
          )
      AND (
           :onlyMine = false
           OR (e IS NOT NULL AND e.employeeId = :empId)
          )
    ORDER BY w.workDate DESC, w.workLogId DESC
    """,
		countQuery = """
    SELECT COUNT(w)
    FROM WorkLog w
    LEFT JOIN w.employee e
    WHERE (
           :deptId IS NULL
           OR (e IS NOT NULL AND e.department.commonCodeId = :deptId)
          )
      AND (
           :onlyMine = false
           OR (e IS NOT NULL AND e.employeeId = :empId)
          )
    """)
	Page<WorkLogResponseDTO> findWithRead(
		@Param("empId") Long empId,
		@Param("deptId") Long deptId,
		@Param("onlyMine") boolean onlyMine,
		Pageable pageable
	);
}
