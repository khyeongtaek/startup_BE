package org.goodee.startup_BE.schedule.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDateTime;

@Table(name="tbl_schedule_participant")

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ScheduleParticipant {

    @Id
    @Column(name = "participant_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;


    // Schedule Table  M : 1
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="schedule_id", referencedColumnName = "schedule_id" , nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="participant", referencedColumnName = "employee_id" , nullable = false)
    private Employee participant;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="participant_status_code_id", referencedColumnName = "common_code_id", nullable = false)
    private CommonCode participantStatus;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean is_deleted;


    public static ScheduleParticipant createScheduleParticipant(
            Schedule schedule,
            Employee participant,
            CommonCode participantStatus
    ) {
        ScheduleParticipant sp = new ScheduleParticipant();
        sp.schedule = schedule;
        sp.participant = participant;
        sp.participantStatus = participantStatus;
        sp.is_deleted = false;
        sp.createdAt = LocalDateTime.now();
        sp.updatedAt = LocalDateTime.now();
        return sp;
    }


    public void delete() {
        this.is_deleted = true;
        this.updatedAt = LocalDateTime.now();
    }


    public void updateStatus(CommonCode newStatus) {
        this.participantStatus = newStatus;
        this.updatedAt = LocalDateTime.now();
    }


}

