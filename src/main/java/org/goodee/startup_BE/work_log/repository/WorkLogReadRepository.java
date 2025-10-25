package org.goodee.startup_BE.work_log.repository;

import org.goodee.startup_BE.work_log.entity.WorkLogRead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkLogReadRepository extends JpaRepository<WorkLogRead, Long> {
}
