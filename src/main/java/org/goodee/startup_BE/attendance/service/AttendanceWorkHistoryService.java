package org.goodee.startup_BE.attendance.service;

import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.attendance.entity.AttendanceWorkHistory;
import org.goodee.startup_BE.employee.entity.Employee;

import java.util.List;

public interface AttendanceWorkHistoryService {

    void recordHistory (Attendance attendance, Employee employee, String actionCode);

    List<AttendanceWorkHistory> getHistoryByEmployee(Long employeeId);
}
