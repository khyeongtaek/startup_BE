package org.goodee.startup_BE.approval.service;

import org.goodee.startup_BE.approval.dto.ApprovalTemplateResponseDTO;
import org.goodee.startup_BE.approval.entity.ApprovalTemplate;
import org.goodee.startup_BE.approval.repository.ApprovalTemplateRepository;

import java.util.List;

public interface ApprovalTemplateService {

    // 삭제되지 않은 모든 결재 양식 조회
    List<ApprovalTemplateResponseDTO> getAllTemplates();

    // 삭제되지 않은 특정 결재 양식 조회
    ApprovalTemplateResponseDTO getTemplate(Long templateId);
}
