package org.goodee.startup_BE.approval.dto;

import lombok.*;
import org.goodee.startup_BE.approval.entity.ApprovalReference;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalReferenceResponseDTO {

    // 참조 고유 ID
    private Long referenceId;

    // 이 참조가 속한 문서 ID
    private Long docId;

    // 참조자가 문서를 열람한 시간
    private LocalDateTime viewedAt;

    // 참조자 정보
    private EmployeeResponseDTO referrer;

    /**
     * ApprovalReference 엔티티를 ApprovalReferenceResponseDTO로 변환하는 정적 메서드
     * @param reference ApprovalReference 엔티티
     * @return ApprovalReferenceResponseDTO 객체
     */
    public static ApprovalReferenceResponseDTO toDTO(ApprovalReference reference) {
        return ApprovalReferenceResponseDTO.builder()
                .referenceId(reference.getReferenceId())
                .docId(reference.getDoc().getDocId())
                .viewedAt(reference.getViewedAt())
                .referrer(EmployeeResponseDTO.toDTO(reference.getEmployee()))
                .build();
    }
}