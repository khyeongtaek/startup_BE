package org.goodee.startup_BE.attendance.repository;

import org.goodee.startup_BE.attendance.entity.AttendanceWorkHistory;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AttendanceWorkHistoryRepository extends JpaRepository<AttendanceWorkHistory, Long> {

}
