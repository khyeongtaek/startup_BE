package org.goodee.startup_BE.approval.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.approval.enums.ApprovalLineStatus;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee; // Employee 임포트
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_approval_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Comment("결재선 고유 ID")
    private Long lineId;

    @Column(nullable = false)
    @Comment("결재 순서")
    private Long approvalOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_status", nullable = false)
    @Comment("결재 상태")
    private CommonCode approvalStatus;

    @Comment("결재 처리일")
    private LocalDateTime approvalDate;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Comment("결재 의견")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    @Comment("문서 ID")
    private ApprovalDoc doc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Comment("결재자 ID")
    private Employee employee;

    public static ApprovalLine createApprovalLine(
            Long approvalOrder, ApprovalDoc doc, Employee employee, CommonCode approvalStatus, LocalDateTime approvalDate, String comment
    ) {
        ApprovalLine line = new ApprovalLine();
        line.approvalOrder = approvalOrder;
        line.doc = doc;
        line.employee = employee;
        line.updateApprovalStatus(approvalStatus);
        line.updateComment(comment);
        line.approvalDate = approvalDate;
        line.comment = comment;
        return line;
    }

    public void updateApprovalStatus(CommonCode approvalStatus) {
        this.approvalStatus = approvalStatus;

        if(ApprovalLineStatus.APPROVED.name().equals(approvalStatus.getValue1())  ||
                ApprovalLineStatus.REJECTED.name().equals(approvalStatus.getValue1())){
            this.approvalDate = LocalDateTime.now();
        }
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }


}