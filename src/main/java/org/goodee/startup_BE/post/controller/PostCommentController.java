package org.goodee.startup_BE.post.controller;

import ch.qos.logback.core.model.conditional.ElseModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.dto.PostCommentRequestDTO;
import org.goodee.startup_BE.post.dto.PostCommentResponseDTO;
import org.goodee.startup_BE.post.service.PostCommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Spring Security 어노테이션
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;


@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "PostComment API", description = "댓글 관련 API")
public class PostCommentController {

    private final PostCommentService postCommentService;
    private final EmployeeRepository employeeRepository;

    // 댓글 등록
    @Operation(summary = "댓글 등록", description = "댓글을 등록합니다")
    @ApiResponse(responseCode = "200", description = "댓글 등록 성공")
    @PostMapping
    public ResponseEntity<APIResponseDTO<PostCommentResponseDTO>> createComment(
            @RequestBody PostCommentRequestDTO postCommentRequestDTO,
            @Parameter(hidden = true) Authentication authentication
    ) {
        String username = authentication.getName();

        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        postCommentRequestDTO.setEmployeeId(employee.getEmployeeId());

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
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody PostCommentRequestDTO postCommentRequestDTO
    ) {

        String username = authentication.getName();

        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        Long employeeId = employee.getEmployeeId();

        PostCommentResponseDTO postCommentResponseDTO =
                postCommentService.updateComment(commentId, employeeId, postCommentRequestDTO);

        return ResponseEntity.ok(APIResponseDTO.<PostCommentResponseDTO>builder()
                .message("댓글 수정 성공")
                .data(postCommentResponseDTO)
                .build());
    }

    // 댓글 삭제
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다")
    @ApiResponse(responseCode = "204", description = "댓글 삭제 성공 - 응답 본문 없음")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @Parameter(hidden = true) Authentication authentication
    ) {
        String username = authentication.getName();
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        Long employeeId = employee.getEmployeeId();

        postCommentService.deleteComment(commentId, employeeId);

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