package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.entity.PostComment;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentRequestDTO {

  // 댓글 등록/ 수정 요청용 DTO
    private Long commentId;
    private Long postId;
    private Long employeeId;
    private String content;

  // DTO -> Entity 변환
  public PostComment toEntity(Post post, Employee employee) {
    return PostComment.builder()
            .post(post)         // 어떤 게시글에 속한 댓글인지.
            .employee(employee) // 누가 작성했는지.
            .content(content)   // 댓글 내용.
            .build();
  }

}
