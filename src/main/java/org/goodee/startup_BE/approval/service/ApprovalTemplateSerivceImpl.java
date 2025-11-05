package org.goodee.startup_BE.approval.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.approval.dto.ApprovalTemplateResponseDTO;
import org.goodee.startup_BE.approval.entity.ApprovalTemplate;
import org.goodee.startup_BE.approval.repository.ApprovalTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 읽기 전용 서비스
public class ApprovalTemplateSerivceImpl implements ApprovalTemplateService{

    private final ApprovalTemplateRepository approvalTemplateRepository;

    /**
     * 삭제되지 않은 모든 결재 양식 조회
     * @return List<ApprovalTemplateResponseDTO>
     */
    @Override
    public List<ApprovalTemplateResponseDTO> getAllTemplates() {

        // Respository에서 삭제되지 않은 템플릿 조회
        List<ApprovalTemplate> templates = approvalTemplateRepository.findAllByIsDeletedFalse();

        // Entity List -> DTO List
        return templates.stream()
                .map(ApprovalTemplateResponseDTO::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 삭제되지 않은 특정 결재 양식 조회
     * (양식의 상세 내용을 미리 볼 때 사용)
     * @param templateId
     * @return ApprovalTemplateResponseDTO
     */
    @Override
    public ApprovalTemplateResponseDTO getTemplate(Long templateId) {

        // 삭제되지 않은 템플릿을 ID로 조회
        ApprovalTemplate template = approvalTemplateRepository.findByTemplateIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new IllegalArgumentException("해당 템플릿을 찾을 수 없습니다"));

        // Entity -> DTO
        return ApprovalTemplateResponseDTO.toDTO(template);
    }

}
