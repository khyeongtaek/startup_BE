package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDTO {

    private Long postId;
    private Long employeeId;
    private String employeeName;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isNotification;
    private Boolean alert;
    private List<AttachmentFileResponseDTO> attachmentFiles;

    // 기본 toDTO (첨부파일 없이)
    public static PostResponseDTO toDTO(Post post) {
        if (post == null) return null;

        return new PostResponseDTO(
                post.getPostId(),
                post.getEmployee() != null ? post.getEmployee().getEmployeeId() : null,
                post.getEmployeeName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getIsNotification(),
                post.getAlert(),
                null // 파일 없음
        );
    }

    // 첨부파일 포함 toDTO
    public static PostResponseDTO toDTO(Post post, List<AttachmentFileResponseDTO> files) {
        if (post == null) return null;

        return new PostResponseDTO(
                post.getPostId(),
                post.getEmployee() != null ? post.getEmployee().getEmployeeId() : null,
                post.getEmployeeName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getIsNotification(),
                post.getAlert(),
                files  // 여기서 파일 세팅
        );
    }
}
