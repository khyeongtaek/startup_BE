package org.goodee.startup_BE.work_log.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "tbl_work_log_read", uniqueConstraints = {@UniqueConstraint(columnNames = {"work_log_id","employee_id"})})
@Getter
public class WorkLogRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Comment("PK")
    private Long readId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_log_id", nullable = false)
    @Comment("읽은 업무일지 ID")
    private WorkLog workLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "employee_id")
    @Comment("읽은 직원 ID")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    protected WorkLogRead() {}

    public static WorkLogRead createWorkLogRead(WorkLog workLog, Employee employee) {
        WorkLogRead workLogRead = new WorkLogRead();
        workLogRead.workLog = workLog;
        workLogRead.employee = employee;
        return workLogRead;
    }
}
