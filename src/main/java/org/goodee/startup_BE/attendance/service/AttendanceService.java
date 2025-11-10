package org.goodee.startup_BE.attendance.service;

import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.entity.Attendance;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    // 오늘 출근 시간 조회
    AttendanceResponseDTO getTodayAttendance(Long employeeId);

    // 출근 등록
    AttendanceResponseDTO clockIn(Long employeeId);

    // 퇴근 등록
    AttendanceResponseDTO clockOut(Long employeeId);

    // 내 근태 내역 조회 (기간)
    List<AttendanceResponseDTO> getMyAttendanceList(Long employeeId, LocalDate start, LocalDate end);

    // 관리자 전체 근태 조회
    List<AttendanceResponseDTO> getAllAttendances();

    // 근무 상태 변경
    String updateWorkStatus(Long employeeId, String statusCode);

    // 주 근무 시간 조회
     Map<String, Object> getWeeklyWorkSummary(Long employeeId);


    @Transactional(readOnly = true)
    Map<String, Object> getAttendanceSummary(Long employeeId);
}
