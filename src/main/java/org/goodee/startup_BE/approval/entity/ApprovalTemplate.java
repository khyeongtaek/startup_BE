package org.goodee.startup_BE.approval.entity;


import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Table(name="tbl_approval_template")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("양식 고유 ID")
    private Long templateId;

    @Column(columnDefinition = "LONGTEXT")
    @Comment("양식 이름")
    private String templateName;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    @Comment("양식 기타 사항")
    private String content;

    @Column(name="created_at")
    @Comment("생성일")
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // 생성 팩토리 메소드

    public static ApprovalTemplate create(String templateName, String content) {
        ApprovalTemplate approvalTemplate = new ApprovalTemplate();

        approvalTemplate.templateName = templateName;
        approvalTemplate.content = content;
        approvalTemplate.isDeleted = false;
        return approvalTemplate;
    }

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 양식 삭제 (소프트 삭제)
    public void softDelete() {
        this.isDeleted = true;
    }
}

