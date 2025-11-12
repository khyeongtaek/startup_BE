package org.goodee.startup_BE.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleParticipantResponseDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;
import org.goodee.startup_BE.schedule.entity.Schedule;
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

    //  일정 등록

    @Operation(summary = "일정 등록", description = "새로운 일정을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
            @ApiResponse(responseCode = "404", description = "직원 또는 공통코드 정보 없음", content = @Content)
    })
    @PostMapping
    public ResponseEntity<APIResponseDTO<ScheduleResponseDTO>> createSchedule(@RequestBody ScheduleRequestDTO request) {
        ScheduleResponseDTO created = scheduleService.createSchedule(request);
        return ResponseEntity.ok(APIResponseDTO.<ScheduleResponseDTO>builder()
                .message("일정 생성 성공")
                .data(created)
                .build());
    }

    // 일정 수정
    @Operation(summary = "일정 수정", description = "일정 ID를 기준으로 일정을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 수정 성공"),
            @ApiResponse(responseCode = "404", description = "해당 일정을 찾을 수 없음", content = @Content)
    })
    @PutMapping("/{scheduleId}")
    public ResponseEntity<APIResponseDTO<ScheduleResponseDTO>> updateSchedule(
            @Parameter(description = "수정할 일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId,
            @RequestBody ScheduleRequestDTO request
    ) {
        ScheduleResponseDTO updated = scheduleService.updateSchedule(scheduleId, request);
        return ResponseEntity.ok(APIResponseDTO.<ScheduleResponseDTO>builder()
                .message("일정 수정 성공")
                .data(updated)
                .build());
    }


    //  전체 일정 조회

    @Operation(summary = "전체 일정 조회", description = "삭제되지 않은 전체 일정을 조회합니다.")
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


    //  단일 일정 조회

    @Operation(summary = "단일 일정 조회", description = "일정 ID를 기준으로 해당 일정을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 조회 성공"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{scheduleId}")
    public ResponseEntity<APIResponseDTO<ScheduleResponseDTO>> getMySchedule(
            @Parameter(description = "조회할 일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId) {
        ScheduleResponseDTO schedule = scheduleService.getSchedule(scheduleId);
        return ResponseEntity.ok(APIResponseDTO.<ScheduleResponseDTO>builder()
                .message("일정 조회 성공")
                .data(schedule)
                .build());
    }


    //  기간별 일정 조회

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


    //  일정 삭제 (Soft Delete)

    @Operation(summary = "일정 삭제 (Soft Delete)", description = "일정 ID를 기준으로 해당 일정을 논리적으로 삭제합니다. (isDeleted = true)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<APIResponseDTO<Void>> deleteSchedule(
            @Parameter(description = "삭제할 일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId) {

        scheduleService.deleteSchedule(scheduleId);

        return ResponseEntity.ok(APIResponseDTO.<Void>builder()
                .message("일정이 성공적으로 삭제되었습니다.")
                .build());
    }

    //  일정 참여자 초대
    @Operation(summary = "일정에 참여자 초대", description = "일정 ID를 기준으로 여러 사원을 초대합니다. 초대된 참여자는 기본적으로 '미응답(PENDING)' 상태로 등록됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참여자 초대 성공"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음", content = @Content)
    })
    @PostMapping("/{scheduleId}/participants")
    public ResponseEntity<APIResponseDTO<Void>> inviteParticipants(
            @Parameter(description = "참여자를 초대할 일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId,
            @Parameter(description = "참여할 사원 ID 리스트", required = true, example = "[2, 3, 4]")
            @RequestBody List<Long> employeeIds
    ) {
        scheduleService.inviteParticipants(scheduleId, employeeIds);

        return ResponseEntity.ok(APIResponseDTO.<Void>builder()
                .message("참여자 초대 완료 (기본 상태: 미응답)")
                .build());
    }

    //  일정 참여자 상태 변경
    @Operation(
            summary = "일정 참여자 상태 변경",
            description = "해당 일정의 특정 참여자의 상태(예: 수락, 거절, 미응답)를 변경합니다. " +
                    "value1 값으로 PS_ATTEND, PS_REJECT, PS_PENDING 등을 전달합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참여자 상태 변경 성공"),
            @ApiResponse(responseCode = "404", description = "참여자 또는 코드 정보를 찾을 수 없음", content = @Content)
    })
    @PutMapping("/{scheduleId}/participants/{employeeId}")
    public ResponseEntity<APIResponseDTO<Void>> updateParticipantStatus(
            @Parameter(description = "상태를 변경할 일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId,
            @Parameter(description = "상태를 변경할 사원 ID", required = true, example = "3")
            @PathVariable Long employeeId,
            @Parameter(description = "변경할 상태 코드 값 (예: ATTEND, REJECT, PENDING)", required = true, example = "ATTEND")
            @RequestParam("value1") String value1
    ){
        scheduleService.updateParticipantStatus(scheduleId, employeeId, value1);

        return ResponseEntity.ok(APIResponseDTO.<Void>builder()
                .message("참여자 상태가 성공적으로 변경되었습니다.")
                .build());
    }

    //  로그인한 사용자의 보이는 일정 조회 (작성자 + 초대받은 일정)

    @Operation(
            summary = "로그인한 사용자의 일정 목록 조회",
            description = "해당 사용자가 작성한 일정 또는 초대받은 일정만 조회합니다. 삭제된 일정은 제외됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 사원 ID로 조회된 일정이 없음", content = @Content)
    })
    @GetMapping("/visible/{employeeId}")
    public ResponseEntity<APIResponseDTO<List<ScheduleResponseDTO>>> getVisibleSchedules(
            @Parameter(description = "조회할 사원의 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        List<ScheduleResponseDTO> schedules = scheduleService.getVisibleSchedules(employeeId);
        return ResponseEntity.ok(
                APIResponseDTO.<List<ScheduleResponseDTO>>builder()
                        .message("작성자 또는 초대된 일정만 조회 완료")
                        .data(schedules)
                        .build()
        );
    }

    //  일정 참여자 조회
    @Operation(summary = "일정 참여자 조회", description = "해당 일정에 초대된 모든 참여자 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참여자 조회 성공"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{scheduleId}/participants")
    public ResponseEntity<APIResponseDTO<List<ScheduleParticipantResponseDTO>>> getParticipants(
            @Parameter(description = "참여자 목록을 조회할 일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId
    ) {
        List<ScheduleParticipantResponseDTO> participants = scheduleService.getParticipants(scheduleId);

        return ResponseEntity.ok(APIResponseDTO.<List<ScheduleParticipantResponseDTO>>builder()
                .message("참여자 조회 성공")
                .data(participants)
                .build());
    }
}