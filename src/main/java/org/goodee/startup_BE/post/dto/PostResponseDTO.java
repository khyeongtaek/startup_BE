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

  // 게시글 응답용 DTO
    private Long postId;
    private Long employeeId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isNotification; // 공지글 여부(true = 공지글)

    // Entity -> DTO 변환
    public static PostResponseDTO toDTO(Post post) {
        if (post == null) {
            return null;
        }

        return new PostResponseDTO(
                post.getPostId(),
                // postComment의 작성자(employee)가 있다면, 그 사람의 ID를 가져오고, 없으면 null을 반환
                post.getEmployee() != null ? post.getEmployee().getEmployeeId() : null,
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getIsNotification()
        );
    }

}
