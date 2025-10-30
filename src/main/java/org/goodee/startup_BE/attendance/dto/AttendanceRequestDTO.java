package org.goodee.startup_BE.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(description = "근태 정보 요청 DTO")
public class AttendanceRequestDTO {

    @Schema(description = "직원 고유 ID", example = "1")
    private Long employeeId;

    @Schema(description = "근무한 날짜", example = "2025-10-28")
    private LocalDate attendanceDate;

    @Schema(description = "출근 시간", example = "2025-10-28T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "퇴근 시간", example = "2025-10-28T18:00:00")
    private LocalDateTime endTime;

    @Schema(description = "근무 상태 코드 ", example = "WORK")
    private String workStatus;

    @Schema(description = "삭제 여부", example = "false")
    private boolean isDeleted;


    public Attendance toEntity(Employee employee, CommonCode workStatus) {
        return Attendance.builder()
                .employee(employee)
                .attendanceDate(attendanceDate)
                .startTime(startTime)
                .endTime(endTime)
                .workStatus(workStatus)
                .isDeleted(isDeleted)
                .build();
    }
}