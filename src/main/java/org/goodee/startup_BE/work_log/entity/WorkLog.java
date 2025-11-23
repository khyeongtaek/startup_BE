package org.goodee.startup_BE.work_log.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;


@Entity
@Table(name = "tbl_work_log")
@Where(clause = "is_deleted = false")
@Getter
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Comment("PK")
    private Long workLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @Comment("작성자(직원) ID")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id", nullable = false)
    @Comment("업무 분류")
    private CommonCode workType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_option_id", nullable = false)
    @Comment("업무별 세부 항목")
    private CommonCode workOption;

    @Column(nullable = false)
    @Comment("작업일 (작성일 X)")
    private LocalDateTime workDate;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("업무일지 제목")
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    @Comment("업무 세부 내용")
    private String content;

    @Column(nullable = false)
    @Comment("삭제 여부")
    private Boolean isDeleted;

    @Column(nullable = true, updatable = false)
    @Comment("생성시각")
    private LocalDateTime createdAt;

    @Column(nullable = true)
    @Comment("수정시각")
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
            Employee employee, CommonCode workType, CommonCode option,
            LocalDateTime workDate, String title, String content
    ) {
        WorkLog workLog = new WorkLog();
        workLog.employee = employee;
        workLog.workType = workType;
        workLog.workOption = option;
        workLog.workDate = workDate;
        workLog.title = title;
        workLog.content = content;
        return workLog;
    }

    public void updateWorkLog(
            CommonCode workType, CommonCode option,
            LocalDateTime workDate, String title, String content
    ) {
        this.workType = workType;
        this.workOption = option;
        this.workDate = workDate;
        this.title = title;
        this.content = content;
    }

    public void deleteWorkLog() {
        this.isDeleted = true;
    }
}
