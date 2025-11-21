package org.goodee.startup_BE.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.schedule.service.ScheduleEmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule Employee API", description = "일정 초대용 직원 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleEmployeeController {

    private final ScheduleEmployeeService scheduleEmployeeService;

    @Operation(summary = "일정 초대용 직원 목록 조회", description = "일정 초대 시 사용할 전체 직원의 ID와 name 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "직원 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/employees")
    public ResponseEntity<APIResponseDTO<List<EmployeeInfoDTO>>> getAllEmployeesForSchedule(
            @Parameter(description = "현재 로그인한 사용자의 name", required = false, example = "user01")
            @RequestParam(required = false) String name
    ) {

        List<EmployeeInfoDTO> employeeList = scheduleEmployeeService.getAllEmployees();

        return ResponseEntity.ok(APIResponseDTO.<List<EmployeeInfoDTO>>builder()
                .message("직원 목록 조회 성공")
                .data(employeeList)
                .build());
    }

    // 내부 DTO (간단한 데이터만 전달하므로 record 사용)
    public record EmployeeInfoDTO(
            @Parameter(description = "직원 ID", example = "1") Long employeeId,
            @Parameter(description = "직원 name", example = "honggildong") String name
    ) {}
}