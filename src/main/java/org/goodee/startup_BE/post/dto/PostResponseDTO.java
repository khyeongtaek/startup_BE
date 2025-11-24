package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
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

    // 추가된 필드들 (작성자 정보)
    private String profileImg;
    private String department;
    private String position;
    private String email;

    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isNotification;
    private Boolean alert;
    private List<AttachmentFileResponseDTO> attachmentFiles;


    // ================================
    // 기본 toDTO (첨부파일 X)
    // ================================
    public static PostResponseDTO toDTO(Post post) {
        if (post == null) return null;

        Employee e = post.getEmployee();

        return new PostResponseDTO(
                post.getPostId(),
                e != null ? e.getEmployeeId() : null,
                post.getEmployeeName(),

                // 추가된 필드 매핑
                e != null ? e.getProfileImg() : null,
                e != null ? e.getDepartment().getValue1() : null,
                e != null ? e.getPosition().getValue1() : null,
                e != null ? e.getEmail() : null,

                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getIsNotification(),
                post.getAlert(),
                null
        );
    }

    // ================================
    // 첨부파일 포함 toDTO
    // ================================
    public static PostResponseDTO toDTO(Post post, List<AttachmentFileResponseDTO> files) {
        if (post == null) return null;

        Employee e = post.getEmployee();

        return new PostResponseDTO(
                post.getPostId(),
                e != null ? e.getEmployeeId() : null,
                post.getEmployeeName(),

                // 추가된 필드 매핑
                e != null ? e.getProfileImg() : null,
                e != null ? e.getDepartment().getValue1() : null,
                e != null ? e.getPosition().getValue1() : null,
                e != null ? e.getEmail() : null,

                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getIsNotification(),
                post.getAlert(),
                files
        );
    }
}
