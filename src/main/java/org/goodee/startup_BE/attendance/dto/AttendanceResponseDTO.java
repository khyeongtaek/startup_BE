package org.goodee.startup_BE.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.attendance.entity.Attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(description = "근태 정보 응답 DTO")
public class AttendanceResponseDTO {

    @Schema(description = "근태 ID", example="1")
    private Long attendanceId;

    @Schema(description = "직원 고유 ID", example = "1")
    private Long employeeId;

    @Schema(description = "직원 이름", example = "홍길동")
    private String employeeName;

    @Schema(description = "근무한 날짜", example = "2025-10-27")
    private LocalDate attendanceDate;

    @Schema(description = "직원 연차 개수", example = "15")
    private Integer annualLeaveCount;

    @Schema(description = "총 근무 일수", example = "30일")
    private Integer workDate;

    @Schema(description = "출근 시간", example = "2025-10-27T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "퇴근 시간", example = "2025-10-27T17:00:00")
    private LocalDateTime endTime;

    @Schema(description = "근무 상태명 (CommonCode ID)", example = "근무")
    private String workStatus;

    @Schema(description = "생성 일시", example = "2025-10-27T17:00:00")
    private LocalDateTime createdAt;


    @Schema(description = "수정 시간", example = "2025-10-27T17:00:00")
    private LocalDateTime updatedAt;

    public static AttendanceResponseDTO toDTO(Attendance attendance){
        return AttendanceResponseDTO.builder()
                .attendanceId(attendance.getAttendanceId())
                .employeeId(attendance.getEmployee().getEmployeeId())
                .employeeName(attendance.getEmployee().getName())
                .attendanceDate(attendance.getAttendanceDate())
                //.annualLeaveCount(attendance.)
                .workDate(attendance.getWorkDate())
                .startTime(attendance.getStartTime())
                .endTime(attendance.getEndTime())
                .workStatus(attendance.getWorkStatus().getCodeDescription())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}
