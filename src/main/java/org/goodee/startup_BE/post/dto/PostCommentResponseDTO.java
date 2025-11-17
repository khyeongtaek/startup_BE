package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.post.entity.PostComment;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentResponseDTO {

  // 게시글 댓글 응답용 DTO
    private Long commentId;
    private Long postId;
    private Long employeeId;
    private String employeename;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

  // Entity -> DTO 변환
  public static PostCommentResponseDTO toDTO(PostComment postComment) {
    if (postComment == null || postComment.getIsDeleted()) {
      return null;
    }

      Long postId = postComment.getPost() != null ? postComment.getPost().getPostId() : null;
      Long employeeId = postComment.getEmployee() != null ? postComment.getEmployee().getEmployeeId() : null;
      String employeeName = postComment.getEmployee() != null ? postComment.getEmployee().getName() : null;

    return new PostCommentResponseDTO(
            postComment.getCommentId(),
            postId,
            employeeId,
            employeeName,
            postComment.getContent(),
            postComment.getCreatedAt(),
            postComment.getUpdatedAt()

    );
  }


}
