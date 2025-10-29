package org.goodee.startup_BE.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.entity.ApprovalReference;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.employee.entity.Employee;

/**
 * 참조자 정보를 위한 Request DTO
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ApprovalReferenceRequestDTO {

    @NotNull(message = "참조 ID는 필수입니다.", groups = {ValidationGroups.Update.class})
    private Long referenceId;

    @NotNull(message = "참조자 ID는 필수입니다.", groups = {ValidationGroups.Create.class})
    private Long referrerId;

    private Long docId;

    /**
     * DTO를 ApprovalReference 엔티티로 변환
     *
     * @param doc      이 참조가 속한 ApprovalDoc 엔티티
     * @param referrer 참조자 Employee 엔티티
     * @return ApprovalReference 엔티티
     */
    public ApprovalReference toEntity(
            ApprovalDoc doc, //  이 참조가 속한 문서 엔티티
            Employee referrer // 참조자 엔티티
    ) {
        return ApprovalReference.createApprovalReference(
                doc,
                referrer
        );
    }

}