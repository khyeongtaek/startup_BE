package org.goodee.startup_BE.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.schedule.entity.ScheduleParticipant;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(description = "일정 참석자 응답 DTO")
public class ScheduleParticipantResponseDTO {

    @Schema(description = "참여자 고유 ID", example = "1001")
    private Long participantId;

    @Schema(description = "일정 ID", example = "10")
    private Long scheduleId;

    @Schema(description = "참여자 직원 ID", example = "2")
    private Long participantEmployeeId;

    @Schema(description = "참여자 이름", example = "김철수")
    private String participantName;

    @Schema(description = "참여 상태명 (CommonCode)", example = "참석")
    private String participantStatusName;

    @Schema(description = "생성일시", example = "2025-10-27T09:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-10-27T10:00:00")
    private LocalDateTime updatedAt;

    public static ScheduleParticipantResponseDTO toDTO(ScheduleParticipant scheduleParticipant){
        return ScheduleParticipantResponseDTO.builder()
                .participantId(scheduleParticipant.getParticipantId())
                .scheduleId(scheduleParticipant.getSchedule().getScheduleId())
                // 삭제된 직원일 경우 employeeId = null
                .participantEmployeeId(
                        scheduleParticipant.getParticipant() != null
                                ? scheduleParticipant.getParticipant().getEmployeeId()
                                : null
                )
                // 삭제된 직원일 경우 이름 = "정보 없음"
                .participantName(
                        scheduleParticipant.getParticipant() != null
                                ? scheduleParticipant.getParticipant().getName()
                                : "정보 없음"
                )
                .participantStatusName(scheduleParticipant.getParticipantStatus().getValue2())
                .createdAt(scheduleParticipant.getCreatedAt())
                .updatedAt(scheduleParticipant.getUpdatedAt())
                .build();
    }
}
