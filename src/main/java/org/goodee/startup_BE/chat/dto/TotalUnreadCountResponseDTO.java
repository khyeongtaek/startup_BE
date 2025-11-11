package org.goodee.startup_BE.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TotalUnreadCountResponseDTO {

    private long totalUnreadCount;
}
