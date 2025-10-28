package org.goodee.startup_BE.notification.dto;

import lombok.*;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.notification.entity.Notification;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NotificationResponseDTO {

    private Long notificationId;
    private OwnerType ownerType;
    private String url;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Boolean readAt;

    public Notification toEntity(Employee employee) {
        return Notification.createNotification(employee, ownerType, url, title, content);
    }

    public static NotificationResponseDTO toDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .notificationId(notification.getNotificationId())
                .ownerType(OwnerType.valueOf(notification.getOwnerType().name()))
                .url(notification.getUrl())
                .title(notification.getTitle())
                .content(notification.getContent())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt() != null ? Boolean.TRUE : Boolean.FALSE)
                .build();
    }
}
