package org.goodee.startup_BE.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.approval.dto.ApprovalTemplateResponseDTO;
import org.goodee.startup_BE.approval.service.ApprovalTemplateService;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ApprovalTemplate API", description = "전자결재 관련 API")
@RestController
@RequestMapping("/api/approval-templates")
@RequiredArgsConstructor
public class ApprovalTemplateController {

    private final ApprovalTemplateService approvalTemplateService;

    @Operation(summary = "결재 양식 전체 조회",
               description = "새 결재 문서 작성시 '양식 선택' 드롭다운 등에 사용할 템를릿 목록을 조회" )
    @GetMapping()
    public ResponseEntity<APIResponseDTO<List<ApprovalTemplateResponseDTO>>> getApprovalTemplates() {
        return ResponseEntity.ok(APIResponseDTO.<List<ApprovalTemplateResponseDTO>>builder()
                .message("결재 양식 목록 조회 성공")
                .data(approvalTemplateService.getAllTemplates())
                .build());
    }

    @Operation(summary = "결재 양식 상세 조회",
    description = "특정 템플릿의 상세 내용을 조회 한다.")
    @GetMapping("/{templateId}")
    public ResponseEntity<APIResponseDTO<ApprovalTemplateResponseDTO>> getApprovalTemplate(
            @Parameter(description = "조회할 템플릿 ID", required = true, example = "1")
            @PathVariable Long templateId
    ) {
        return ResponseEntity.ok(APIResponseDTO.<ApprovalTemplateResponseDTO>builder()
                .message("결재 양식 상세 조회 성공")
                .data(approvalTemplateService.getTemplate(templateId))
                .build());
    }
}
