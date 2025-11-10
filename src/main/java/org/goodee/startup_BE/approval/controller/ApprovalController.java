package org.goodee.startup_BE.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.approval.dto.ApprovalDocRequestDTO;
import org.goodee.startup_BE.approval.dto.ApprovalDocResponseDTO;
import org.goodee.startup_BE.approval.dto.ApprovalLineRequestDTO;
import org.goodee.startup_BE.approval.service.ApprovalService;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Approval API", description = "결재(전자결재) 관련 API") // 태그 설정
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalService approvalService;

    @Operation(summary = "결재 양식 템플릿 목록 조회", description = "새 결재 작성 시 사용할 수 있는 양식 템플릿 목록을 조회 한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결재 양식 목록 조회 성공")
    })
    @GetMapping("/templates")
    public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getApprovalTemplates() {
        return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
                .message("결재 양식 목록 조회 성공")
                .data(approvalService.getAllApprovalTemplates())
                .build());
    }

    @Operation(summary = "결재 문서 상신 (생성)",
            description = "새로운 결재 문서를 상신(제출). 결재선(approvalLines)과 참조자(approvalReferences) 정보를 포함해야 한다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "결재 문서 생성 요청 DTO",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ApprovalDocRequestDTO.class))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결재 상신 성공")
    })
    @PostMapping
    public ResponseEntity<APIResponseDTO<ApprovalDocResponseDTO>> createApproval(
            @Validated({ValidationGroups.Create.class})
            @ModelAttribute ApprovalDocRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(APIResponseDTO.<ApprovalDocResponseDTO>builder()
                .message("결재 상신 성공")
                .data(approvalService.createApproval(request, authentication.getName()))
                .build());
    }

    @Operation(summary = "결재 처리 (승인/반려)",
            description = "대기중인 본인의 결재 항목(결재선)에 대해 승인 또는 반려 처리.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "결재 처리 요청 DTO",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ApprovalLineRequestDTO.class))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결재 처리 성공")
    })
    @PatchMapping("/decide")
    public ResponseEntity<APIResponseDTO<ApprovalDocResponseDTO>> decideApproval(
            @Validated({ValidationGroups.Update.class})
            @RequestBody ApprovalLineRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(APIResponseDTO.<ApprovalDocResponseDTO>builder()
                .message("결재 처리 성공")
                .data(approvalService.decideApproval(request, authentication.getName()))
                .build());
    }

    @Operation(summary = "결재 문서 상세 조회", description = "특정 결재 문서의 상세 내용을 조회. (결재선, 참조자 포함)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결재 문서 조회 성공")
    })
    @GetMapping("/{docId}")
    public ResponseEntity<APIResponseDTO<ApprovalDocResponseDTO>> getApproval(
            @Parameter(description = "조회할 결재 문서의 ID (docId)", required = true, example = "1")
            @PathVariable Long docId,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(APIResponseDTO.<ApprovalDocResponseDTO>builder()
                .message("결재 문서 조회 성공")
                .data(approvalService.getApproval(docId, authentication.getName()))
                .build());
    }

    @Operation(summary = "결재 대기 문서 조회", description = "내가 결재해야 할 (현재 내 차례인) 문서 목록을 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결재 대기 목록 조회 성공")
    })
    @GetMapping("/pending")
    public ResponseEntity<APIResponseDTO<Page<ApprovalDocResponseDTO>>> getPendingApprovals(
            @Parameter(hidden = true) Authentication authentication,
            // 이 API는 서비스 로직에서 정렬 순서가 고정되어 있으므로, 여기서는 정렬(sort) 기본값을 지정하지 않음
            @PageableDefault(page = 0, size = 10)
            @Parameter(description = "페이징 정보 (예: ?page=0&size=10)")
            Pageable pageable
    ) {
        return ResponseEntity.ok(APIResponseDTO.<Page<ApprovalDocResponseDTO>>builder()
                .message("결재 대기 목록 조회 성공")
                .data(approvalService.getPendingApprovals(pageable, authentication.getName()))
                .build());
    }

    @Operation(summary = "결재 기안 문서 조회", description = "내가 상신한 (기안한) 문서 목록을 조회.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기안 문서 목록 조회 성공")
    })
    @GetMapping("/drafted")
    public ResponseEntity<APIResponseDTO<Page<ApprovalDocResponseDTO>>> getDraftedDocuments(
            @Parameter(hidden = true) Authentication authentication,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이징 정보 (예: ?page=0&size=10&sort=createdAt,desc)")
            Pageable pageable
    ) {
        return ResponseEntity.ok(APIResponseDTO.<Page<ApprovalDocResponseDTO>>builder()
                .message("기안 문서 목록 조회 성공")
                .data(approvalService.getDraftedDocuments(pageable, authentication.getName()))
                .build());
    }

    @Operation(summary = "결재 참조 문서 조회", description = "내가 참조자로 지정된 문서 목록을 조회.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참조 문서 목록 조회 성공")
    })
    @GetMapping("/referenced")
    public ResponseEntity<APIResponseDTO<Page<ApprovalDocResponseDTO>>> getReferencedDocuments(
            @Parameter(hidden = true) Authentication authentication,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이징 정보 (예: ?page=0&size=10&sort=createdAt,desc)")
            Pageable pageable
    ) {
        return ResponseEntity.ok(APIResponseDTO.<Page<ApprovalDocResponseDTO>>builder()
                .message("참조 문서 목록 조회 성공")
                .data(approvalService.getReferencedDocuments(pageable, authentication.getName()))
                .build());
    }

    @Operation(summary = "결재 완료 문서 조회", description = "내 결재가 포함된 (기안, 결재, 참조) 문서 중 '최종 승인' 또는 '최종 반려'된 목록을 조회.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "완료 문서 목록 조회 성공")
    })
    @GetMapping("/completed")
    public ResponseEntity<APIResponseDTO<Page<ApprovalDocResponseDTO>>> getCompletedDocuments(
            @Parameter(hidden = true) Authentication authentication,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이징 정보 (예: ?page=0&size=10&sort=createdAt,desc)")
            Pageable pageable
    ) {
        return ResponseEntity.ok(APIResponseDTO.<Page<ApprovalDocResponseDTO>>builder()
                .message("완료 문서 목록 조회 성공")
                .data(approvalService.getCompletedDocuments(pageable, authentication.getName()))
                .build());
    }
}