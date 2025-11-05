package org.goodee.startup_BE.approval.dto;

import lombok.*;
import org.goodee.startup_BE.approval.entity.ApprovalTemplate;

import java.time.LocalDateTime;

@Getter @Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTemplateResponseDTO {

    // 양식 고유 ID
    private Long templateId;

    // 양식 이름
    private String templateName;

    // 양식 기타 사항
    private String content;

    // 생성일
    private LocalDateTime createdAt;

    /**
     * ApprovalTemplate 엔티티를 ApprovalTemplateResponseDTO로 변환하는 정적 메서드
     * @param template ApprovalTemplate 엔티티
     * @return ApprovalTemplateResponseDTO 객체
     */
    public static ApprovalTemplateResponseDTO toDTO(ApprovalTemplate template) {
        return ApprovalTemplateResponseDTO.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .content(template.getContent())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
