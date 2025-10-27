package org.goodee.startup_BE.schedule;


import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDateTime;

@Table(name="tbl_schedule")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="schedule_id")
    private Long scheduleId;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_id", referencedColumnName = "employee_id", nullable = false)
    private Employee employee;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "schedule_category_id", referencedColumnName = "common_code_id", nullable = false)
    private CommonCode category;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String place;

    // CommonCode Table  M : 1
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "color_code_id", referencedColumnName = "common_code_id", nullable = false)
    private CommonCode color;

    @Column(name="start_time")
    private LocalDateTime startTime;

    @Column(name="end_time")
    private LocalDateTime endTime;


    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;


    @Column(name = "is_deleted")
    private Boolean is_deleted;


    public static Schedule createSchedule(
            Employee employee,
            String title,
            String content,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        Schedule schedule = new Schedule();
        schedule.employee = employee;
        schedule.title = title;
        schedule.content = content;
        schedule.startTime = startTime;
        schedule.endTime = endTime;
        schedule.is_deleted = false;
        schedule.createdAt = LocalDateTime.now();
        schedule.updatedAt = LocalDateTime.now();
        return schedule;
    }

    public void delete() {
        this.is_deleted = true;
        this.updatedAt = LocalDateTime.now();
    }


    public void update(String title, String content, LocalDateTime startTime, LocalDateTime endTime) {
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }


}
