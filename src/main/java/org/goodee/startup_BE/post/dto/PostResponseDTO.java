package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.post.entity.Post;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDTO {

    private Long postId;
    private Long employeeId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isNotification;

    // Entity → DTO 변환
    public static PostResponseDTO toDTO(Post post) {
        if (post == null) {
            return null;
        }

        return new PostResponseDTO(
                post.getPostId(),
                post.getEmployee() != null ? post.getEmployee().getEmployeeId() : null,
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getIsNotification()
        );
    }

}
