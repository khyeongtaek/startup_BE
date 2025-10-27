package org.goodee.startup_BE.approval.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_approval_doc")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalDoc {

    @Id
    @Column(nullable = false)
    @Comment("문서 고유 ID")
    private Long docId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("제목")
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("내용")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_status", nullable = false)
    @Comment("문서 상태")
    private CommonCode docStatus;

    @Column(nullable = false)
    @Comment("작성일")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정일")
    private LocalDateTime updatedAt;

    @Comment("시작날짜")
    private LocalDateTime startDate;

    @Comment("종료날짜")
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @Comment("기안자 ID")
    private Employee employee;

    //아직 테이블이 없음
//    @Column(name = "template_id", nullable = false)
//    @Comment("문서 양식 ID")
//    private Long templateId;

    // --- 생성 팩토리 메서드 ---
    public static ApprovalDoc createApprovalDoc(
            Long docId
            , String title
            , String content
            , Employee employee
//            , Long templateId
            , LocalDateTime startDate
            , LocalDateTime endDate
            , CommonCode docStatus
    ) {
        ApprovalDoc doc = new ApprovalDoc();
        doc.setDocId(docId);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setEmployee(employee);
//        doc.setTemplateId(templateId);
        doc.setStartDate(startDate);
        doc.setEndDate(endDate);
        doc.setDocStatus(docStatus);
        return doc;
    }

    public void update(String title, String content, LocalDateTime startDate, LocalDateTime endDate, CommonCode docStatus) {
        this.setTitle(title);
        this.setContent(content);
        this.setStartDate(startDate);
        this.setEndDate(endDate);
        this.setDocStatus(docStatus);
    }

    @PrePersist
    protected void onPrePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }




}