package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentRequestDTO {

    private Long commentId;
    private Long postId;
    private Long employeeId;
    private String content;

}
