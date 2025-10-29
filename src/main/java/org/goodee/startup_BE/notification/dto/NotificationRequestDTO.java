package org.goodee.startup_BE.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.notification.entity.Notification;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {
    private String employee;
    private Long ownerTypeCommonCodeId;
    private String url;
    private String title;
    private String content;

    public Notification toEntity(Employee employee, CommonCode ownerTypeCode) {
        return Notification.createNotification(employee, ownerTypeCode, url, title, content);
    }
}
