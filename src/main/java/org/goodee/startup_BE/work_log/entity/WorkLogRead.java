package org.goodee.startup_BE.work_log.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "tbl_work_log_read", uniqueConstraints = {@UniqueConstraint(columnNames = {"work_log_id","employee_id"})})
@Getter
public class WorkLogRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "read_id", nullable = false)
    private Long readId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_log_id", nullable = false)
    private WorkLog workLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    protected WorkLogRead() {}

    public static WorkLogRead createWorkLogRead(WorkLog workLog, Employee employee) {
        WorkLogRead workLogRead = new WorkLogRead();
        workLogRead.workLog = workLog;
        workLogRead.employee = employee;
        return workLogRead;
    }
}
