package org.goodee.startup_BE.employee.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_employee_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    @Comment("변경 이력 고유 ID")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Comment("변경 대상 직원")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updater_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Comment("변경 수행 관리자")
    private Employee updater;

    @Column(nullable = false)
    @Comment("변경된 필드명 (예: 재직상태, 부서, 직급)")
    private String fieldName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Comment("변경 전 값")
    private String oldValue;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Comment("변경 후 값")
    private String newValue;

    @Column(nullable = false, updatable = false)
    @Comment("변경 발생 시각")
    private LocalDateTime changedAt;

    // --- 생성 팩토리 메서드 ---
    public static EmployeeHistory createHistory(
            Employee employee, Employee updater, String fieldName,
            String oldValue, String newValue
    ) {
        EmployeeHistory history = new EmployeeHistory();
        history.employee = employee;
        history.updater = updater;
        history.fieldName = fieldName;
        history.oldValue = oldValue;
        history.newValue = newValue;
        return history;
    }

    @PrePersist
    protected void onPrePersist() {
        changedAt = LocalDateTime.now();
    }
}