package org.goodee.startup_BE.post.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDTO {

    private Long PostId;
    private Long employeeId;
    private String title;
    private String content;
    private boolean isNotification;

}
