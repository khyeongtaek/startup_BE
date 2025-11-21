package org.goodee.startup_BE.attendance.entity;


import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


import java.time.LocalDate;
import java.time.LocalDateTime;


@Table(name="tbl_attendance")
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="attendance_id")
    private Long attendanceId;


    // Employee Table  M : 1
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="create_employee", referencedColumnName = "employee_id", nullable= true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;


    // 누적 근무 일수
    @Column(name="work_date", nullable = false)
    private Long workDate;

    // 근무한 날짜
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;


    @Column(name = "is_deleted")
    private Boolean isDeleted;

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
    @JoinColumn(name = "work_status", referencedColumnName = "common_code_id")
    private CommonCode workStatus;


    public static Attendance createAttendance(Employee employee, LocalDate attendanceDate, CommonCode workStatus) {
        Attendance attendance = new Attendance();
        attendance.employee = employee;
        attendance.attendanceDate = attendanceDate;
        attendance.workStatus = workStatus;
        attendance.workDate = 1L;
        attendance.isDeleted = false;
        attendance.createdAt = LocalDateTime.now();
        attendance.updatedAt = LocalDateTime.now();
        return attendance;
    }

    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeWorkStatus(CommonCode workStatus) {
        this.workStatus = workStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void setWorkDate(Long workDate) {
        this.workDate = workDate;
    }
}

