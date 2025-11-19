package org.goodee.startup_BE.approval.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_approval_reference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Comment("참조 고유 ID")
    private Long referenceId;

    @Comment("참조자가 문서를 열람한 시간")
    private LocalDateTime viewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    @Comment("참조할 문서 ID")
    private ApprovalDoc doc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Comment("참조자 ID")
    private Employee employee;

    // --- 생성 팩토리 메서드 ---
    public static ApprovalReference createApprovalReference(
            ApprovalDoc doc, Employee employee
    ) {
        ApprovalReference reference = new ApprovalReference();
        reference.doc = doc;
        reference.employee = employee;
        return reference;
    }

    public void update(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}