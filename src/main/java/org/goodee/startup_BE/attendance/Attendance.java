package org.goodee.startup_BE.attendance;


import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;


import java.time.LocalDateTime;


@Table(name="tbl_attendance")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="attendance_id")
    private Long attendanceId;


    // Employee Table  M : 1
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="create_employee", referencedColumnName = "employee_id", nullable= false)
    private Employee employee;


    @Column(name="work_date", nullable = false)
    private LocalDateTime workDate;

    @Column(name = "is_deleted")
    private Boolean is_deleted;

    @Column(name="start_time")
    private LocalDateTime startTime;

    @Column(name="end_time")
    private LocalDateTime endTime;


    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    // template_id, status_code, update_employee
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_status", referencedColumnName = "code_id")
    private CommonCode workStatus;


    public static Attendance createAttendance(Employee employee, LocalDateTime workDate) {
        Attendance attendance = new Attendance();
        attendance.employee = employee;
        attendance.workDate = workDate;
        attendance.is_deleted = false;
        attendance.createdAt = LocalDateTime.now();
        attendance.updatedAt = LocalDateTime.now();
        return attendance;
    }

    public void delete() {
        this.is_deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }
}

