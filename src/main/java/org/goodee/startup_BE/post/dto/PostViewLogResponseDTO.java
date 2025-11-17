package org.goodee.startup_BE.post.dto;

import lombok.*;
import org.goodee.startup_BE.post.entity.PostViewLog;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostViewLogResponseDTO {

    private Long postViewLogId;
    private Long postId;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime viewedAt;

    // Entity -> DTO 변환
    public static PostViewLogResponseDTO toDTO(PostViewLog postViewLog) {
        if (postViewLog == null) {
            return null;
        }

        return PostViewLogResponseDTO.builder()
                .postViewLogId(postViewLog.getPostViewLogId())
                .postId(postViewLog.getPost().getPostId())
                .employeeId(postViewLog.getEmployee().getEmployeeId())
                .employeeName(postViewLog.getEmployee().getName())
                .viewedAt(postViewLog.getViewedAt())
                .build();
    }

}
