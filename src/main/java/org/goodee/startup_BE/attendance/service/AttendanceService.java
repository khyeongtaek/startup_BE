package org.goodee.startup_BE.attendance.service;

import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;

import java.time.LocalDate;
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

    // 주 근무 시간 조회 (기본: 이번 주)
    Map<String, Object> getWeeklyWorkSummary(Long employeeId);

    // 주 근무 시간 조회 (임의 주 시작일 지정)
    Map<String, Object> getWeeklyWorkSummary(Long employeeId, LocalDate weekStart);

    // 근태 요약 정보
    Map<String, Object> getAttendanceSummary(Long employeeId);

    // 휴가 / 반차 등록 메소드
    void markVacation(Long employeeId, LocalDate date, String vacationType);


     List<LocalDate> getAbsentDays(Long employeeId, int year, int month);
}