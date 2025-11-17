package org.goodee.startup_BE.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.post.dto.PostRequestDTO;
import org.goodee.startup_BE.post.dto.PostResponseDTO;
import org.goodee.startup_BE.post.service.PostService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Post API", description = "게시글 관련 API")
public class PostController {

    private final PostService postService;


    // 게시글 검색
    @Operation(summary = "게시글 검색", description = "게시글을 검색합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 검색 성공")
    @PostMapping("/search/{commonCodeId}")
    public ResponseEntity<APIResponseDTO<List<PostResponseDTO>>> searchPost(
            @RequestBody PostRequestDTO postRequestDTO,
            @NotNull(message = "게시글 ID는 필수입니다.")
            @PathVariable Long commonCodeId,
            @PageableDefault(page = 0, size = 10)
            @Parameter(description = "페이징 정보 (예: ?page=0&size=10&sort=createdAt,desc)")
            Pageable pageable
    ) {
        postRequestDTO.setCommonCodeId(commonCodeId);
        List<PostResponseDTO> postList = postService.searchPost(postRequestDTO, pageable);
        return ResponseEntity.ok(APIResponseDTO.<List<PostResponseDTO>>builder()
                .message("게시글 검색 성공")
                .data(postList)
                .build());
    }


    // 게시글 생성
    @Operation(summary = "게시글 생성", description = "새로운 게시글을 생성합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 생성 성공")
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity <APIResponseDTO<PostResponseDTO>> createPost(
            @Validated({ValidationGroups.Create.class})
            @ModelAttribute PostRequestDTO postRequestDTO
    ) {
        PostResponseDTO responseDTO = postService.createPost(postRequestDTO);

        return ResponseEntity.ok(APIResponseDTO.<PostResponseDTO>builder()
                .message("게시글 생성 성공")
                .data(responseDTO)
                .build());
    }
    // 게시글 수정
    @Operation(summary = "게시글 수정", description = "게시글의 내용, 제목, 공지 등을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 수정 성공")
    @PutMapping(value = "/{postId}", consumes = {"multipart/form-data"})
    public ResponseEntity <APIResponseDTO<PostResponseDTO>> updatePost(
            @Validated({ValidationGroups.Update.class})
            @PathVariable Long postId,
            @ModelAttribute PostRequestDTO postRequestDTO
    ) {
        // postRequestDTO 내부에 직접 postId세팅
        postRequestDTO.setPostId(postId);

        // 서비스에 postRequestDTO 하나만 전달
        PostResponseDTO responseDTO = postService.updatePost(postRequestDTO);

        return ResponseEntity.ok(APIResponseDTO.<PostResponseDTO>builder()
                .message("게시글 수정 성공")
                .data(responseDTO)
                .build());
    }
    // 게시글 삭제
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제처리합니다.")
    @ApiResponse(responseCode = "204", description = "게시글 삭제 성공")
    @DeleteMapping("/{postId}")
    public ResponseEntity <APIResponseDTO<Void>> deletePost(@PathVariable Long postId) {
          postService.deletePost(postId);
          return  ResponseEntity.ok(APIResponseDTO.<Void>builder()
                  .message("게시글 삭제 성공")
                  .build());
    }

}
