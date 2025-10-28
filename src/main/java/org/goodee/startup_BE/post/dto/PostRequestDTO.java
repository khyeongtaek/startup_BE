package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.post.entity.Post;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDTO {

    private Long postId;
    private Long employeeId;
    private String title;
    private String content;
    private boolean isNotification;

    // DTO -> Entity 변환
    public Post toEntity(Employee employee) {
        return Post.builder()
                .postId(postId)
                .employee(employee)
                .title(title)
                .content(content)
                .isNotification(isNotification)
                .build();
    }
}
