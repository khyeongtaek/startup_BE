package org.goodee.startup_BE.approval.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Lombok Setter 임포트 (필드 레벨에서 사용할 것이므로 유지)
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_approval_doc")
@Getter
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
    @JoinColumn(name = "creater_id", nullable = false)
    @Comment("기안자 ID")
    private Employee creater;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updater_id")
    @Comment("수정자")
    private Employee updater;

    //아직 테이블이 없음
//    @Column(name = "template_id", nullable = false)
//    @Comment("문서 양식 ID")
//    private Long templateId;

    // --- 생성 팩토리 메서드 ---
    public static ApprovalDoc createApprovalDoc(
            String title
            , String content
            , Employee creater
//            , Long templateId
            , LocalDateTime startDate
            , LocalDateTime endDate
            , CommonCode docStatus
    ) {
        ApprovalDoc doc = new ApprovalDoc();
        doc.updateTitle(title);
        doc.updateContent(content);
        doc.creater = creater;
//        doc.setTemplateId(templateId);
        doc.updateStartDate(startDate);
        doc.updateEndDate(endDate);
        doc.updateDocStatus(docStatus);
        return doc;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void updateEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void updateDocStatus(CommonCode docStatus) {
        this.docStatus = docStatus;
    }

    public void updateUpdater(Employee updater) {
        this.updater = updater;
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