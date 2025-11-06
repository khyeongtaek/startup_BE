package org.goodee.startup_BE.approval.dto;

import lombok.*;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDocResponseDTO {

    // 문서 고유 ID
    private Long docId;

    // 제목
    private String title;

    // 내용
    private String content;

    // 문서 상태
    private CommonCodeResponseDTO docStatus;

    // 결재 양식
    private CommonCodeResponseDTO approvalTemplate;

    // 작성일
    private LocalDateTime createdAt;

    // 수정일
    private LocalDateTime updatedAt;

    // 시작날짜
    private LocalDateTime startDate;

    // 종료날짜
    private LocalDateTime endDate;

    // 기안자 정보 (DTO로 변환)
    private EmployeeResponseDTO creator;

    // 수정자 정보 (DTO로 변환, null 가능)
    private EmployeeResponseDTO updater;

    // 결재선 목록 (DTO 리스트)
    private List<ApprovalLineResponseDTO> approvalLines;

    // 참조자 목록 (DTO 리스트)
    private List<ApprovalReferenceResponseDTO> approvalReferences;

    /**
     * 엔티티와 DTO 리스트를 조합하여 최종 Response DTO를 생성
     * (서비스 레이어에서 사용)
     *
     * @param doc ApprovalDoc 엔티티
     * @param lineList ApprovalLineResponseDTO 리스트
     * @param referenceList ApprovalReferenceResponseDTO 리스트
     * @return ApprovalDocResponseDTO 객체
     */
    public static ApprovalDocResponseDTO toDTO(
            ApprovalDoc doc,
            List<ApprovalLineResponseDTO> lineList,
            List<ApprovalReferenceResponseDTO> referenceList
    ) {
        return ApprovalDocResponseDTO.builder()
                .docId(doc.getDocId())
                .title(doc.getTitle())
                .content(doc.getContent())
                .docStatus(CommonCodeResponseDTO.toDTO(doc.getDocStatus()))
                .approvalTemplate(CommonCodeResponseDTO.toDTO(doc.getApprovalTemplate()))
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .startDate(doc.getStartDate())
                .endDate(doc.getEndDate())
                .creator(EmployeeResponseDTO.toDTO(doc.getCreator()))
                .updater(doc.getUpdater() != null ? EmployeeResponseDTO.toDTO(doc.getUpdater()) : null)
                .approvalLines(lineList)
                .approvalReferences(referenceList)
                .build();
    }
}