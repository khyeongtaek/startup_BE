package org.goodee.startup_BE.attendance.service;

import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.entity.Attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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


}
