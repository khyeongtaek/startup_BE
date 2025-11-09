package org.goodee.startup_BE.approval.service;

import org.goodee.startup_BE.approval.dto.ApprovalDocRequestDTO;
import org.goodee.startup_BE.approval.dto.ApprovalDocResponseDTO;
import org.goodee.startup_BE.approval.dto.ApprovalLineRequestDTO;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApprovalService {

    //상신
    ApprovalDocResponseDTO createApproval(ApprovalDocRequestDTO request, String username);

    //결재 상세
    ApprovalDocResponseDTO getApproval(Long approvalDocId, String username);

    // 결재 승인/반려 - 결재선 정보를 받고, 결재 승인/반려에 대한 결과로 결재 상세를 반환
    ApprovalDocResponseDTO decideApproval(ApprovalLineRequestDTO request, String username);

    // 결재 대기 문서 조회 - 본인이 결재해야할 문서 목록
    Page<ApprovalDocResponseDTO> getPendingApprovals(Pageable pageable, String username);

    // 결재 기안 문서 조회 - 본인이 기안한 문서 목록
    Page<ApprovalDocResponseDTO> getDraftedDocuments(Pageable pageable, String username);

    // 결재 참조 문서 조회 - 본인이 참조자로 지정된 문서 목록
    Page<ApprovalDocResponseDTO> getReferencedDocuments(Pageable pageable, String username);

    // 결재 완료 문서 조회 - 본인이 결재자로 참여했던 문서중 최종 처리가 완료된 목록
    Page<ApprovalDocResponseDTO> getCompletedDocuments(Pageable pageable, String username);

    // 모든 결재양식 템플릿 목록을 조회
    List<CommonCodeResponseDTO> getAllApprovalTemplates();

}
