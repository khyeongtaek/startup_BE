package org.goodee.startup_BE.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponseDTO {

    private Long notificationId;
    private Long employeeId;
    private Long refId;
    private String title;
    private String content;
    private LocalDateTime createAt;
    private LocalDateTime readAt;
    private boolean isDeleted;
}
