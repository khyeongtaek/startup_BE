package org.goodee.startup_BE.approval.dto;

import lombok.*;
import org.goodee.startup_BE.approval.entity.ApprovalLine;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalLineResponseDTO {

    // 결재선 고유 ID
    private Long lineId;

    // 이 결재선이 속한 문서 ID
    private Long docId;

    // 결재 순서
    private Long approvalOrder;

    // 결재 상태
    private CommonCodeResponseDTO approvalStatus;

    // 결재 처리일
    private LocalDateTime approvalDate;

    // 결재 의견
    private String comment;

    // 결재자 정보
    private EmployeeResponseDTO approver;

    /**
     * ApprovalLine 엔티티를 ApprovalLineResponseDTO로 변환하는 정적 메서드
     * @param line ApprovalLine 엔티티
     * @return ApprovalLineResponseDTO 객체
     */
    public static ApprovalLineResponseDTO toDTO(ApprovalLine line) {
        return ApprovalLineResponseDTO.builder()
                .lineId(line.getLineId())
                .docId(line.getDoc().getDocId())
                .approvalOrder(line.getApprovalOrder())
                .approvalStatus(CommonCodeResponseDTO.toDTO(line.getApprovalStatus()))
                .approvalDate(line.getApprovalDate())
                .comment(line.getComment())
                .approver(EmployeeResponseDTO.toDTO(line.getEmployee()))
                .build();
    }
}