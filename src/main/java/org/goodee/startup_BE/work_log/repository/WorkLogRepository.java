package org.goodee.startup_BE.work_log.repository;

import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

}
