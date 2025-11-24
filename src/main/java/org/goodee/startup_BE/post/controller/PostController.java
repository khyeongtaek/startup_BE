package org.goodee.startup_BE.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.post.dto.PostRequestDTO;
import org.goodee.startup_BE.post.dto.PostResponseDTO;
import org.goodee.startup_BE.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Post API", description = "게시글 관련 API")
public class PostController {

    private final PostService postService;


    // 게시글 검색
    @Operation(summary = "게시글 검색", description = "게시글을 검색합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 검색 성공")
    @GetMapping("/search/{commonCodeCode}")
    public ResponseEntity<APIResponseDTO<Page<PostResponseDTO>>> searchPost(
            @NotNull(message = "게시글 ID는 필수입니다.")
            @PathVariable String commonCodeCode,
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 10)
            @Parameter(description = "페이징 정보 (예: ?page=0&size=10&sort=createdAt,desc)")
            Pageable pageable
    ) {
        Page<PostResponseDTO> postList = postService.searchPost(commonCodeCode, keyword, pageable);
        return ResponseEntity.ok(APIResponseDTO.<Page<PostResponseDTO>>builder()
                .message("")
                .data(postList)
                .build());
    }

    // 게시글 상세
    @Operation(summary = "게시글 상세", description = "게시글의 상세 내용을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 상세 조회 성공")
    @GetMapping(value = "/{postId}")
    public ResponseEntity<APIResponseDTO<PostResponseDTO>> getPostDetail(
            @PathVariable Long postId
    ) {
        PostResponseDTO responseDTO = postService.getPostDetail(postId);
        return ResponseEntity.ok(
                APIResponseDTO.<PostResponseDTO>builder()
                        .message("게시글 상세 조회 성공")
                        .data(responseDTO)
                        .build()
        );
    }


    // 게시글 생성
    @Operation(summary = "게시글 생성", description = "새로운 게시글을 생성합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 생성 성공")
    @PostMapping(value = "/{commonCodeCode}", consumes = {"multipart/form-data"})
    public ResponseEntity<APIResponseDTO<PostResponseDTO>> createPost(
            @PathVariable String commonCodeCode,
            @ModelAttribute PostRequestDTO postRequestDTO,  // title, content, isNotification, alert
            @RequestPart(value = "multipartFile", required = false) List<MultipartFile> multipartFile,
            @Parameter(hidden = true) Authentication authentication
    ) {
        String username = authentication.getName();

        // ModelAttribute에서 바인딩된 파일이 있을 수 있으므로 무조건 null 처리
        postRequestDTO.setMultipartFile(null);

        // RequestPart에서 받은 파일을 직접 주입
        postRequestDTO.setMultipartFile(multipartFile);

        PostResponseDTO responseDTO = postService.createPost(postRequestDTO, commonCodeCode, username);

        return ResponseEntity.ok(APIResponseDTO.<PostResponseDTO>builder()
                .message("게시글 생성 성공")
                .data(responseDTO)
                .build());
    }


    // 게시글 수정
    @Operation(summary = "게시글 수정", description = "게시글의 내용, 제목, 공지 등을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 수정 성공")
    @PutMapping(value = "/{postId}", consumes = {"multipart/form-data"})
    public ResponseEntity<APIResponseDTO<PostResponseDTO>> updatePost(
            @PathVariable Long postId,
            @ModelAttribute PostRequestDTO postRequestDTO,
            @RequestPart(value = "multipartFile", required = false) List<MultipartFile> multipartFiles,
            @RequestPart(value = "deleteFileIds", required = false) List<Long> deleteFileIds,
            @Parameter(hidden = true) Authentication authentication
    ) {
        String username = authentication.getName();

        postRequestDTO.setPostId(postId);
        postRequestDTO.setMultipartFile(multipartFiles); // 새 파일 목록
        postRequestDTO.setDeleteFileIds(deleteFileIds);  // 삭제할 파일 목록

        PostResponseDTO responseDTO = postService.updatePost(postRequestDTO, username);

        return ResponseEntity.ok(
                APIResponseDTO.<PostResponseDTO>builder()
                        .message("게시글 수정 성공")
                        .data(responseDTO)
                        .build()
        );
    }

    // 게시글 삭제
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제처리합니다.")
    @ApiResponse(responseCode = "204", description = "게시글 삭제 성공")
    @DeleteMapping("/{postId}")
    public ResponseEntity<APIResponseDTO<Void>> deletePost(
            @PathVariable Long postId,
            @Parameter(hidden = true) Authentication authentication
    ) {
        String username = authentication.getName();

        postService.deletePost(postId, username);

        return ResponseEntity.ok(APIResponseDTO.<Void>builder()
                .message("게시글 삭제 성공")
                .build());
    }


}
