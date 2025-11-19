package org.goodee.startup_BE.schedule.entity;


import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @JoinColumn(name = "created_id", referencedColumnName = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
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


    @Column(name="start_time")
    private LocalDateTime startTime;

    @Column(name="end_time")
    private LocalDateTime endTime;


    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;


    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleParticipant> participants = new ArrayList<>();

    public static Schedule createSchedule(
            Employee employee,
            String title,
            String content,
            CommonCode category,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        Schedule schedule = new Schedule();
        schedule.employee = employee;
        schedule.title = title;
        schedule.content = content;
        schedule.category = category;
        schedule.startTime = startTime;
        schedule.endTime = endTime;
        schedule.isDeleted = false;
        schedule.createdAt = LocalDateTime.now();
        schedule.updatedAt = LocalDateTime.now();
        return schedule;
    }

    public void addParticipant(ScheduleParticipant participant){
        this.participants.add(participant);
    }

    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
        this.participants.forEach(ScheduleParticipant::delete);
    }


    public void update(String title, String content, LocalDateTime startTime, LocalDateTime endTime, CommonCode category) {
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        if (category != null) {
            this.category = category;
        }
        this.updatedAt = LocalDateTime.now();
    }


}
