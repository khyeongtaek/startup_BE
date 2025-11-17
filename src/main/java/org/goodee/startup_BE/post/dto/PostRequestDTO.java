package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.post.entity.Post;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDTO {

// 게시글 등록 / 수정 요청시 클라이언트로부터 전달받는 데이터 DTO
    private Long postId;
    private Long employeeId;    // 생성시 필수
    private String employeeName;
    private String title;  // 생성시 필수
    private String content;  //생성시 필수
    private Boolean isNotification; // 공지글 여부 (true = 공지글)  ?
    private Boolean alert;    // 전체 공지 여부
    private Long commonCodeId; // 게시판 ID 추가, 생성시 필수
    private List<MultipartFile> multipartFile;  // 첨부파일

    // DTO -> Entity 변환
    public Post toEntity(Employee employee, CommonCode commonCode) {
        return Post.builder()
                .employee(employee)  // 작성자
                .commonCode(commonCode)
                .title(title)        // 제목
                .content(content)    // 내용
                .isNotification(isNotification != null ? isNotification : false)  // 공지 여부
                .alert(alert != null ? alert : false)
                .build();
    }
}
