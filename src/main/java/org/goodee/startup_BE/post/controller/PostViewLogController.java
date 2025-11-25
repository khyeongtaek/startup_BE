package org.goodee.startup_BE.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.dto.PostViewLogRequestDTO;
import org.goodee.startup_BE.post.dto.PostViewLogResponseDTO;
import org.goodee.startup_BE.post.service.PostViewLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/post/views")
@RequiredArgsConstructor
@Tag(name = "PostViewLog API", description = "조회수 관련 API")
public class PostViewLogController {

    private final PostViewLogService postViewLogService;
    private final EmployeeRepository employeeRepository;

    // 조회수 1 증가
    @Operation(summary = "조회수 증가", description = "게시글 진입시 조회수를 증가시킵니다.")
    @ApiResponse(responseCode = "200", description = "조회수 증가 성공")
    @PostMapping
    public ResponseEntity<APIResponseDTO<PostViewLogResponseDTO>> createPostViewLog(
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody PostViewLogRequestDTO postViewLogRequestDTO

    ) {

        String username = authentication.getName();

        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        Long employeeId = employee.getEmployeeId();

        PostViewLogResponseDTO postViewLogResponseDTO =
                postViewLogService.createPostViewLog(postViewLogRequestDTO, employeeId);

        return ResponseEntity.ok(APIResponseDTO.<PostViewLogResponseDTO>builder()
                .message("조회수 증가 성공")
                .data(postViewLogResponseDTO)
                .build());
    }

    // 조회수 조회
    @Operation(summary = "조회수 조회", description = "특적 게시글의 조회수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회수 조회 성공")
    @GetMapping("/{postId}")
    public ResponseEntity<APIResponseDTO<Long>> getViewCount(
            @PathVariable Long postId
    ) {
        long count = postViewLogService.getViewCount(postId);

        return ResponseEntity.ok(APIResponseDTO.<Long>builder()
                .message("조회수 조회 성공")
                .data(count)
                .build());
    }

}