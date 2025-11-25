package org.goodee.startup_BE.post.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.dto.PostCommentRequestDTO;
import org.goodee.startup_BE.post.dto.PostCommentResponseDTO;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.entity.PostComment;
import org.goodee.startup_BE.post.repository.PostCommentRepository;
import org.goodee.startup_BE.post.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommentServiceImpl implements PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final EmployeeRepository employeeRepository;

    // 댓글 등록
    @Override
    public PostCommentResponseDTO createComment(PostCommentRequestDTO postCommentRequestDTO) {
        Post post = postRepository.findById(postCommentRequestDTO.getPostId())
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        Employee employee = employeeRepository.findById(postCommentRequestDTO.getEmployeeId())
                .orElseThrow(() -> new NoSuchElementException("작성자를 찾을 수 없습니다."));

        PostComment postComment = PostComment.createPostComment(
                post, employee, postCommentRequestDTO.getContent()
        );
        // 저장
        PostComment saved = postCommentRepository.save(postComment);

        return PostCommentResponseDTO.toDTO(saved);
    }
    // 댓글 수정
    @Override
    public PostCommentResponseDTO updateComment(Long commentId, Long employeeId, PostCommentRequestDTO postCommentRequestDTO) {
        PostComment postComment = postCommentRepository.findById(commentId)
            .orElseThrow(() -> new NoSuchElementException("수정할 댓글을 찾을 수 없습니다."));
        if (postComment.getIsDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }
        // 요청한 사용자가 댓글 작성자인지 확인
        if (!postComment.getEmployee().getEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }
        // 내용 변경
        postComment.update(postCommentRequestDTO.getContent());

        return PostCommentResponseDTO.toDTO(postComment);
    }

    // 댓글 삭제
    @Override
    public boolean deleteComment(Long commentId, Long employeeId) {
        PostComment postComment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        if (postComment.getIsDeleted()) {
            throw new IllegalStateException("이미 삭제된 댓글입니다.");
        }

        if (!postComment.getEmployee().getEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException(("댓글 삭제 권한이 없습니다."));
        }
        postComment.delete();

        return true;
    }

    // 특정 게시글의 댓글 조회
    @Override
    @Transactional(readOnly = true)
    public Page<PostCommentResponseDTO> getCommentsByPostId(Long postId, Pageable pageable) {
    // 게시글 존재 유무
    postRepository.findById(postId)
            .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

    // 삭제되지 않은 댓글만 조회.
    Page<PostComment> commentList = postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtDesc(postId, pageable);

    return commentList.map(PostCommentResponseDTO::toDTO);

    }
}