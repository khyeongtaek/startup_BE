package org.goodee.startup_BE.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.schedule.entity.Schedule;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(description = "일정 요청 DTO")
public class ScheduleRequestDTO {

    @Schema(description = "제목", example = "회의")
    private String title;

    @Schema(description = "내용", example = "팀 회의 진행")
    private String content;

    @Schema(description = "카테고리 코드 (CommonCode 참조)", example = "SCH_CATEGORY_MEETING")
    private String categoryCode;

    @Schema(description = "색상 코드 (CommonCode 참조)", example = "COLOR_BLUE")
    private String colorCode;

    @Schema(description = "작성자 직원 ID", example = "1")
    private Long employeeId;

    @Schema(description = "시작 시간", example = "2025-10-27T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "종료 시간", example = "2025-10-27T10:00:00")
    private LocalDateTime endTime;

    @Schema(description = "삭제 여부", example = "false")
    private boolean isDeleted;

    public Schedule toEntity(Employee employee, CommonCode category, CommonCode color) {
        return Schedule.builder()
                .title(title)
                .content(content)
                .category(category)
                .color(color)
                .employee(employee)
                .startTime(startTime)
                .endTime(endTime)
                .isDeleted(isDeleted)
                .build();
    }
}