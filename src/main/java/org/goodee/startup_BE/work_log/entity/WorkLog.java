package org.goodee.startup_BE.work_log.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;


@Entity
@Table(name = "tbl_work_log")
@Getter
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_log_id", nullable = false)
    private Long workLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id", nullable = false)
    private WorkType workType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private WorkTypeOption option;

    @Column(name = "work_date", nullable = false)
    private LocalDateTime workDate;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onPrePersist() {
        if(isDeleted == null) isDeleted = false;
        if(createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    protected WorkLog() {}
    public static WorkLog createWorkLog(
            Employee employee, WorkType workType, WorkTypeOption option,
            LocalDateTime workDate, String title, String content
    ) {
        WorkLog workLog = new WorkLog();
        workLog.employee = employee;
        workLog.workType = workType;
        workLog.option = option;
        workLog.workDate = workDate;
        workLog.title = title;
        workLog.content = content;
        return workLog;
    }

    public void deleteWorkLog() {
        this.isDeleted = true;
    }
}
