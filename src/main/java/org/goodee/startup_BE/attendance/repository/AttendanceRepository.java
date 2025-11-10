package org.goodee.startup_BE.attendance.repository;

import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByEmployeeEmployeeId(Long employeeId);

    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);


    Optional<Attendance> findByEmployeeEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    @Query("SELECT a FROM Attendance a WHERE a.employee.employeeId = :employeeId AND a.attendanceDate = CURRENT_DATE AND a.isDeleted = false")
    Optional<Attendance> findCurrentWorkingRecord(Long employeeId);

    List<Attendance> findByEmployeeEmployeeIdAndAttendanceDateBetween(Long employeeEmployeeId, LocalDate attendanceDateAfter, LocalDate attendanceDateBefore);

    List<Attendance> findByIsDeletedIsFalse();

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.employeeId = :employeeId AND a.isDeleted = false")
    Long countByEmployeeEmployeeId(Long employeeId);

    @Query("SELECT a FROM Attendance a " +
            "WHERE a.employee.employeeId = :employeeId " +
            "AND a.attendanceDate BETWEEN :startOfWeek AND :endOfWeek " +
            "AND a.isDeleted = false " +
            "AND a.startTime IS NOT NULL " +
            "AND a.endTime IS NOT NULL")
    List<Attendance> findWeeklyRecords(Long employeeId, LocalDate startOfWeek, LocalDate endOfWeek);


    // 지각 횟수 체크
    @Query("SELECT COUNT(DISTINCT a.attendanceDate) " +
            "FROM Attendance a " +
            "JOIN AttendanceWorkHistory h ON h.attendance.attendanceId = a.attendanceId " +
            "WHERE a.employee.employeeId = :employeeId " +
            "AND a.attendanceDate BETWEEN :startOfWeek AND :endOfWeek " +
            "AND h.actionCode.value1 = 'LATE' " +
            "AND a.isDeleted = false")
    Long countLatesThisWeek(Long employeeId, LocalDate startOfWeek, LocalDate endOfWeek);
}
