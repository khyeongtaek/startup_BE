package org.goodee.startup_BE.attendance.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.attendance.entity.ApprovalTemplate;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(description = "결재 템플릿 응답 DTO")
public class ApprovalTemplateResponseDTO {

    @Schema(description = "템플릿 고유 ID" , example = "1")
    private Long templateId;

    @Schema(description = "템플릿 이름", example = "연차 신청서")
    private String templateName;

    @Schema(description = "작성자 ID", example = "1")
    private Long createdById;

    @Schema(description = "작성자 이름", example = "1")
    private String createdByName;

    @Schema(description = "생성 일시", example = "2025-10-27T17:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2025-10-27T17:00:00")
    private LocalDateTime updatedAt;

    public static ApprovalTemplateResponseDTO toDTO(ApprovalTemplate approvalTemplate){
        return ApprovalTemplateResponseDTO.builder()
                .templateId(approvalTemplate.getTemplateId())
                .templateName(approvalTemplate.getTemplateName())
                .createdById(approvalTemplate.getCreatedBy().getEmployeeId())
                .createdByName(approvalTemplate.getCreatedBy().getName())
                .createdAt(approvalTemplate.getCreatedAt())
                .updatedAt(approvalTemplate.getUpdatedAt())
                .build();
    }



}
