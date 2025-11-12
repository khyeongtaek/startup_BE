package org.goodee.startup_BE.attendance.repository;

import org.goodee.startup_BE.attendance.entity.AttendanceWorkHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AttendanceWorkHistoryRepository extends JpaRepository<AttendanceWorkHistory, Long> {

    // 전체 이력 조회
    List<AttendanceWorkHistory> findByEmployeeEmployeeIdOrderByActionTimeDesc(Long employeeId);

    // 최근 이력 1건 조회
    AttendanceWorkHistory findTopByEmployeeEmployeeIdOrderByActionTimeDesc(Long employeeId);

    // 오늘 출근 레코드(Attendance) 기준으로만 이력 조회
    List<AttendanceWorkHistory> findByAttendanceAttendanceIdOrderByActionTimeDesc(Long attendanceId);
}
