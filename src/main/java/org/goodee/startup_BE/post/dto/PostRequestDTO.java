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

// 게시글 등록 / 수정 요청시 클라이언트로부터 전달받는 데이터 DTO
    private Long postId;
    private Long employeeId;
    private String title;
    private String content;
    private boolean isNotification; // 공지글 여부 (true = 공지글)

    // DTO -> Entity 변환
    public Post toEntity(Employee employee) {
        return Post.builder()
                .postId(postId)
                .employee(employee)  // 작성자
                .title(title)        // 제목
                .content(content)    // 내용
                .isNotification(isNotification)  // 공지 여부
                .build();
    }
}
