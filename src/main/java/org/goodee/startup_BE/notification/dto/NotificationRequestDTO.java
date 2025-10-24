package org.goodee.startup_BE.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {
    private Long employeeId;
    private Long refId;
    private String title;
    private String content;
}
