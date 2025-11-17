package org.goodee.startup_BE.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.post.dto.PostCommentRequestDTO;
import org.goodee.startup_BE.post.dto.PostCommentResponseDTO;
import org.goodee.startup_BE.post.service.PostCommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Spring Security 어노테이션
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "PostComment API", description = "댓글 관련 API")
public class PostCommentController {

    private final PostCommentService postCommentService;

    // 댓글 등록
    @Operation(summary = "댓글 등록", description = "댓글을 등록합니다")
    @ApiResponse(responseCode = "200", description = "댓글 등록 성공")
    @PostMapping
    public ResponseEntity<APIResponseDTO<PostCommentResponseDTO>> createComment
    (@RequestBody PostCommentRequestDTO postCommentRequestDTO) {
        PostCommentResponseDTO postCommentResponseDTO = postCommentService.createComment(postCommentRequestDTO);

        return ResponseEntity.ok(APIResponseDTO.<PostCommentResponseDTO>builder()
                .message("댓글 등록 성공")
                .data(postCommentResponseDTO)
                .build());
    }

    // 댓글 수정
    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다")
    @ApiResponse(responseCode = "200", description = "댓글 수정 성공")
    @PutMapping("/{commentId}")
    public ResponseEntity <APIResponseDTO<PostCommentResponseDTO>> updateComment (
            @PathVariable Long commentId,
            // @AuthenticationPrincipal을 사용하여 토큰에서 안전하게 employeeId를 추출
            @AuthenticationPrincipal Long employeeId,
            @RequestBody PostCommentRequestDTO postCommentRequestDTO
    ) {
        // employeeId는 이미 Spring Security에 의해 토큰 검증 후 안전하게 추출된 값입니다.
        PostCommentResponseDTO postCommentResponseDTO = postCommentService.updateComment(commentId, employeeId, postCommentRequestDTO);

        return ResponseEntity.ok(APIResponseDTO.<PostCommentResponseDTO>builder()
                .message("댓글 수정 성공")
                .data(postCommentResponseDTO)
                .build());
    }

    // 댓글 삭제
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다")
    @ApiResponse(responseCode = "204", description = "댓글 삭제 성공 - 응답 본문 없음")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment (
            @PathVariable Long commentId
    ) {
        postCommentService.deleteComment(commentId);

        // 204 No Content 반환
        return ResponseEntity.noContent().build();
    }

    // 게시글의 댓글 조회
    @Operation(summary = "댓글 조회", description = "게시글의 댓글을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 댓글 조회")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<APIResponseDTO<Page<PostCommentResponseDTO>>>getCommentsByPostId(
            @PathVariable Long postId,
            Pageable pageable
    ) {
        Page<PostCommentResponseDTO> postCommentResponseDTO = postCommentService.getCommentsByPostId(postId, pageable);

        return ResponseEntity.ok(APIResponseDTO.<Page<PostCommentResponseDTO>>builder()
                .message("댓글 조회 성공")
                .data(postCommentResponseDTO)
                .build());
    }

}