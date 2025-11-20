package org.goodee.startup_BE.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.attendance.entity.AnnualLeave;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드 자동 제외
public class AnnualLeaveResponseDTO {

    private Long leaveId;
    private Long employeeId;
    private String employeeName;
    private Double totalDays;
    private Double usedDays;
    private Double remainingDays;
    private Long year;

    public static AnnualLeaveResponseDTO toDTO(AnnualLeave entity) {
        AnnualLeaveResponseDTOBuilder builder = AnnualLeaveResponseDTO.builder()
                .leaveId(entity.getLeaveId())
                .totalDays(entity.getTotalDays())
                .usedDays(entity.getUsedDays())
                .remainingDays(entity.getRemainingDays())
                .year(entity.getYear());

        if (entity.getEmployee() != null) {
            builder.employeeId(entity.getEmployee().getEmployeeId());
            builder.employeeName(entity.getEmployee().getName());
        }

        return builder.build();
    }
}