package org.goodee.startup_BE.notification.dto;

import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.notification.entity.Notification;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class NotificationRequestDTO {
    private Long employeeId;
    private Long ownerTypeCommonCodeId;
    private String url;
    private String title;
    private String content;

    public Notification toEntity(Employee employee, CommonCode ownerTypeCode) {
        return Notification.createNotification(employee, ownerTypeCode, url, title, content);
    }
}
