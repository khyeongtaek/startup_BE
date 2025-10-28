package org.goodee.startup_BE.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.notification.entity.Notification;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {
    private Long employeeId;
    private OwnerType ownerType;
    private String url;
    private String title;
    private String content;

    public Notification toEntity(Employee employee) {
        return Notification.createNotification(employee, ownerType, url, title, content);
    }
}
