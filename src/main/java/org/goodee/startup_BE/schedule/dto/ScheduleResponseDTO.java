package org.goodee.startup_BE.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.schedule.entity.Schedule;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(description = "일정 응답 DTO")
public class ScheduleResponseDTO {

    @Schema(description = "일정 고유 ID", example = "1")
    private Long scheduleId;

    @Schema(description = "제목", example = "연차")
    private String title;

    @Schema(description = "내용", example = "연차 사용")
    private String content;

    @Schema(description = "카테고리명 (CommonCode)", example = "연차")
    private String categoryName;


    @Schema(description = "작성자 ID", example = "1")
    private Long employeeId;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String employeeName;

    @Schema(description = "시작 시간", example = "2025-10-27T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "종료 시간", example = "2025-10-27T10:00:00")
    private LocalDateTime endTime;

    @Schema(description = "생성일시", example = "2025-10-27T09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-10-27T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;


    public static ScheduleResponseDTO toDTO(Schedule schedule){
        return ScheduleResponseDTO.builder()
                .scheduleId(schedule.getScheduleId())
                .title(schedule.getTitle())
                .content(schedule.getContent())
                .categoryName(schedule.getCategory().getValue2())
                .employeeId(schedule.getEmployee().getEmployeeId())
                .employeeName(schedule.getEmployee().getName())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
