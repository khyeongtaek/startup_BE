package org.goodee.startup_BE.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.employee.entity.EmployeeHistory;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "직원 정보 변경 이력 응답 DTO")
public class EmployeeHistoryResponseDTO {
    @Schema(description = "변경 이력 고유 id", example = "user123")
    private Long historyId;

    @Schema(description = "변경 대상 직원 아이디", example = "user123")
    private String employeeUsername;

    @Schema(description = "변경 수행자 아이디", example = "admin456")
    private String updaterUsername;

    @Schema(description = "변경된 필드명", example = "position")
    private String fieldName;

    @Schema(description = "변경 전 값", example = "사원")
    private String oldValue;

    @Schema(description = "변경 후 값", example = "대리")
    private String newValue;

    @Schema(description = "변경 일시", example = "2025-11-11T10:00:00")
    private LocalDateTime changedAt;

    public static EmployeeHistoryResponseDTO toDTO(EmployeeHistory history) {
        return EmployeeHistoryResponseDTO.builder()
                .historyId(history.getHistoryId())
                .employeeUsername(history.getEmployee().getUsername())
                .updaterUsername(history.getUpdater().getUsername())
                .fieldName(history.getFieldName())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .changedAt(history.getChangedAt())
                .build();
    }
}