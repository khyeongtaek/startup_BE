package org.goodee.startup_BE.attendance.repository;

import org.goodee.startup_BE.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {


    /**
     * 특정 직원의 모든 출근 기록 조회
     */
    List<Attendance> findByEmployeeEmployeeId(Long employeeId);

    /**
     * 특정 날짜의 모든 출근 기록 조회
     */
    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);

    /**
     * 직원 + 날짜로 출근 기록 1건 조회
     * - 출근 여부 체크, 근무 상태 업데이트 등에서 사용
     */
    Optional<Attendance> findByEmployeeEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);


    // ---------------- 출근 중인 기록 조회 ----------------

    /**
     * 오늘 날짜 + 직원 ID 기준으로 "현재 출근 중인" 기록 조회
     * - 동일 날짜 중복 출근 방지
     */
    @Query("SELECT a FROM Attendance a WHERE a.employee.employeeId = :employeeId AND a.attendanceDate = CURRENT_DATE AND a.isDeleted = false")
    Optional<Attendance> findCurrentWorkingRecord(Long employeeId);


    // ---------------- 기간 조회 ----------------

    /**
     * 특정 주간(startOfWeek ~ endOfWeek)의 출근 기록 조회
     * - 주간 근무 현황, 통계에 사용
     */
    List<Attendance> findByEmployeeEmployeeIdAndAttendanceDateBetween(Long employeeEmployeeId, LocalDate attendanceDateAfter, LocalDate attendanceDateBefore);


    /**
     * 삭제되지 않은 모든 Attendance 조회
     * - 관리자 조회용
     */
    List<Attendance> findByIsDeletedIsFalse();


    // ---------------- 총 근무일(정상 출근일) 계산 ----------------

    /**
     * 해당 직원의 "근무한 날 수"를 계산하는 메서드
     *
     * workStatus.value1 기준:
     *  - NORMAL(정상 출근)
     *  - LATE(지각)
     *  - EARLY_LEAVE(조퇴)
     *
     * 즉, 근무일수 = NORMAL + LATE + EARLY_LEAVE
     * 휴가/결근/외근 등은 포함되지 않음.
     */
    @Query("SELECT COUNT(a) " +
            "FROM Attendance a " +
            "WHERE a.employee.employeeId = :employeeId " +
            "AND a.workStatus.value1 IN ('NORMAL', 'LATE', 'EARLY_LEAVE') " +
            "AND a.isDeleted = false")
    Long countByEmployeeEmployeeId(Long employeeId);


    // ---------------- 주간 조회 ----------------

    /**
     * 특정 주(week) 날짜 범위의 출근 기록 조회
     * - 주간 뷰(UI)에서 사용
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.employee.employeeId = :employeeId " +
            "AND a.attendanceDate BETWEEN :startOfWeek AND :endOfWeek " +
            "AND a.isDeleted = false")
    List<Attendance> findWeeklyRecords(Long employeeId, LocalDate startOfWeek, LocalDate endOfWeek);


    // ---------------- 지각 횟수(주간) ----------------

    /**
     * 특정 주간 동안 발생한 "지각" 횟수 조회
     *
     * DISTINCT a.attendanceDate → 같은 날 여러 번 기록돼도 1번만 카운트
     *
     * actionCode.value1 = 'LATE' → WorkHistory에서 지각으로 기록된 경우
     */
    @Query("SELECT COUNT(DISTINCT a.attendanceDate) " +
            "FROM Attendance a " +
            "JOIN AttendanceWorkHistory h ON h.attendance.attendanceId = a.attendanceId " +
            "WHERE a.employee.employeeId = :employeeId " +
            "AND a.attendanceDate BETWEEN :startOfWeek AND :endOfWeek " +
            "AND h.actionCode.value1 = 'LATE' " +
            "AND a.isDeleted = false")
    Long countLatesThisWeek(Long employeeId, LocalDate startOfWeek, LocalDate endOfWeek);


    // 출근 기록 존재 여부 확인
    boolean existsByEmployeeEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);
}