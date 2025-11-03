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
    Integer countByEmployeeEmployeeId(Long employeeId);
}
