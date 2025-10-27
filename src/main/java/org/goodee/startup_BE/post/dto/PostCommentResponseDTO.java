package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentResponseDTO {

    private Long commentId;
    private Long employeeId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
