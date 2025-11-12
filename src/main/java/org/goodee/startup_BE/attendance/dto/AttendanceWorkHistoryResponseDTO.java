package org.goodee.startup_BE.attendance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceWorkHistoryResponseDTO {

    private Long historyId;
    private String employeeName;
    private String actionCode;
    private LocalDateTime actionTime;

}