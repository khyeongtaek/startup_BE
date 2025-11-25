package org.goodee.startup_BE.post.service;

import org.goodee.startup_BE.post.dto.PostCommentRequestDTO;
import org.goodee.startup_BE.post.dto.PostCommentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostCommentService {

    // 댓글 등록
    PostCommentResponseDTO createComment(PostCommentRequestDTO postCommentRequestDTO);

    // 댓글 수정
    PostCommentResponseDTO updateComment(Long commentId, Long employeeId, PostCommentRequestDTO postCommentRequestDTO);

    // 댓글 삭제
    boolean deleteComment(Long commentId, Long employeeId);

    // 특정 게시글의 댓글 조회
    Page<PostCommentResponseDTO> getCommentsByPostId(Long postId, Pageable pageable);
}
