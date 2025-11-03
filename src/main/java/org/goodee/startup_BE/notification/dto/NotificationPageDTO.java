package org.goodee.startup_BE.notification.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class NotificationPageDTO {

    private final List<NotificationResponseDTO> content;
    private final Boolean last;

    public NotificationPageDTO(List<NotificationResponseDTO> content, Boolean last) {
        this.content = content;
        this.last = last;
    }
}
