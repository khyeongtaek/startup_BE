package org.goodee.startup_BE.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;
import org.goodee.startup_BE.schedule.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Schedule API", description = "일정 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;


    //일정 등록
    @Operation(summary = "일정 등록", description = "새로운 일정을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
            @ApiResponse(responseCode = "404", description = "직원 또는 공통코드 정보 없음", content = @Content)
    })
    @PostMapping
    public ResponseEntity<APIResponseDTO<ScheduleResponseDTO>> createSchedule(@RequestBody ScheduleRequestDTO request ){
        ScheduleResponseDTO created = scheduleService.createSchedule(request);
        return ResponseEntity.ok(APIResponseDTO.<ScheduleResponseDTO>builder()
                .message("일정 생성 성공")
                .data(created)
                .build());
    }

    // 전체 일정 조회
    @Operation(summary = "전체 일정 조회", description = "전체 일정을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전체 일정 조회 성공")
    })
    @GetMapping
    public ResponseEntity<APIResponseDTO<List<ScheduleResponseDTO>>> getAllSchedules() {
        List<ScheduleResponseDTO> schedules = scheduleService.getAllSchedule();
        return ResponseEntity.ok(APIResponseDTO.<List<ScheduleResponseDTO>>builder()
                .message("전체 일정 조회 성공")
                .data(schedules)
                .build());
    }

    // 단일 일정 조회
    @Operation(summary = "단일 일정 조회", description = "일정 ID를 기준으로 해당 일정을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 조회 성공"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{scheduleId}")
    public ResponseEntity<APIResponseDTO<ScheduleResponseDTO>> getMySchedule(
            @Parameter(description = "조회할 일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId){
        ScheduleResponseDTO schedule = scheduleService.getSchedule(scheduleId);
        return ResponseEntity.ok(APIResponseDTO.<ScheduleResponseDTO>builder()
                .message("일정 조회 성공")
                .data(schedule)
                .build());
    }

    // 기간별 일정 조회
    @Operation(summary = "기간별 일정 조회", description = "시작일과 종료일을 기준으로 해당 기간의 일정을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기간별 일정 조회 성공"),
            @ApiResponse(responseCode = "400", description = "시작일 또는 종료일이 잘못됨", content = @Content)
    })
    @GetMapping("/period")
    public ResponseEntity<APIResponseDTO<List<ScheduleResponseDTO>>> getSchedulesByPeriod(
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", required = true, example = "2025-10-01")
            @RequestParam LocalDate start,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", required = true, example = "2025-10-31")
            @RequestParam LocalDate end
    ) {
        List<ScheduleResponseDTO> schedules = scheduleService.getAllScheduleByPeriod(start, end);
        return ResponseEntity.ok(APIResponseDTO.<List<ScheduleResponseDTO>>builder()
                .message("기간별 일정 조회 성공")
                .data(schedules)
                .build());

    }
}
