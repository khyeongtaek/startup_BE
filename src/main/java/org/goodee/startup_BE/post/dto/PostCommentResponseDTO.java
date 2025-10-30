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
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

  // Entity -> DTO 변환
  public static PostCommentResponseDTO toDTO(PostComment postComment) {
    if (postComment == null) {
      return null;
    }
    return new PostCommentResponseDTO(
            postComment.getCommentId(),
            // postComment가 어떤 게시글(Post)에 달린 댓글이라면 그 게시글의 ID(postId)를 가져오고,
            // 만약 postComment.getPost()가 null이면 그냥 null을 반환.
            postComment.getPost() != null ? postComment.getPost().getPostId() : null,
            // postComment의 작성자(employee)가 있다면, 그 사람의 ID를 가져오고, 없으면 null을 반환
            postComment.getEmployee() != null ? postComment.getEmployee().getEmployeeId() : null,
            postComment.getContent(),
            postComment.getCreatedAt(),
            postComment.getUpdatedAt()


    );
  }


}
