package org.goodee.startup_BE.approval.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.entity.ApprovalLine;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDateTime;

/**
 * 결재선 정보를 위한 Request DTO
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ApprovalLineRequestDTO {

    @NotNull(message = "결재선 ID는 필수입니다.", groups = {ValidationGroups.Update.class})
    private Long lineId;

    @NotNull(message = "결재 문서 ID는 필수입니다.")
    private Long docId;

    @NotNull(message = "결재 순서는 필수입니다.", groups = ValidationGroups.Create.class)
    @Min(value = 1, message = "결재 순서는 1 이상이어야 합니다.", groups = ValidationGroups.Create.class)
    private Long approvalOrder;

    @NotNull(message = "결재자 ID는 필수입니다.", groups = ValidationGroups.Create.class)
    private Long approverId;

    @NotNull(message = "결재선 상태는 필수입니다.", groups = {ValidationGroups.Update.class})
    private Long statusCodeId;

    private String comment;

    /**
     * DTO를 ApprovalLine 엔티티로 변환
     *
     * @param doc            이 결재선이 속한 ApprovalDoc 엔티티
     * @param approver       결재자 Employee 엔티티
     * @param approvalStatus 결재 상태 CommonCode 엔티티 (예: '대기')
     * @return ApprovalLine 엔티티
     */
    public ApprovalLine toEntity(
            ApprovalDoc doc, // 이 결재선이 속한 문서 엔티티
            Employee approver, // 이 결재선의 결재자 엔티티
            CommonCode approvalStatus // 초기 결재 상태 (예: '대기')
    ) {
        // 엔티티의 정적 팩토리 메서드 호출
        return ApprovalLine.createApprovalLine(
                this.approvalOrder,
                doc,
                approver,
                approvalStatus,
                null, // 결재일(approvalDate)은 생성 시점엔 null
                this.comment
        );
    }
}