package org.goodee.startup_BE.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.dto.AttendanceWorkHistoryResponseDTO;
import org.goodee.startup_BE.attendance.entity.AttendanceWorkHistory;
import org.goodee.startup_BE.attendance.service.AttendanceService;
import org.goodee.startup_BE.attendance.service.AttendanceWorkHistoryService;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Attendance API", description = "근태 관리 API")
@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceWorkHistoryService attendanceWorkHistoryService;

    //  오늘 근태 조회
    @Operation(summary = "오늘 출근 기록 조회", description = "사원 ID를 기준으로 오늘의 출근 기록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "출근 기록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "해당 사원의 출근 기록이 존재하지 않음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/today/{employeeId}")
    public ResponseEntity<APIResponseDTO<AttendanceResponseDTO>> getTodayAttendance(
            @Parameter(description = "조회할 사원 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        AttendanceResponseDTO getAttendance = attendanceService.getTodayAttendance(employeeId);

        return ResponseEntity.ok(APIResponseDTO.<AttendanceResponseDTO>builder()
                .message("오늘 출근 기록 조회 성공")
                .data(getAttendance)
                .build());
    }

    //  출근 등록
    @Operation(summary = "출근 등록", description = "사원의 출근 시간을 등록합니다. 이미 출근 기록이 존재하면 오류가 발생합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "출근 처리 완료"),
            @ApiResponse(responseCode = "400", description = "이미 출근 기록이 존재함", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PostMapping("/clock-in/{employeeId}")
    public ResponseEntity<APIResponseDTO<AttendanceResponseDTO>> clockIn(
            @Parameter(description = "출근 처리할 사원 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        AttendanceResponseDTO clockIn = attendanceService.clockIn(employeeId);

        return ResponseEntity.ok(APIResponseDTO.<AttendanceResponseDTO>builder()
                .message("출근 처리 완료")
                .data(clockIn)
                .build());
    }

    //  퇴근 등록
    @Operation(summary = "퇴근 등록", description = "사원의 퇴근 시간을 기록합니다. 출근 기록이 없을 경우 오류가 발생합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "퇴근 처리 완료"),
            @ApiResponse(responseCode = "400", description = "출근 기록이 존재하지 않음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PostMapping("/clock-out/{employeeId}")
    public ResponseEntity<APIResponseDTO<AttendanceResponseDTO>> clockOut(
            @Parameter(description = "퇴근 처리할 사원 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        AttendanceResponseDTO clockOut = attendanceService.clockOut(employeeId);


        return ResponseEntity.ok(APIResponseDTO.<AttendanceResponseDTO>builder()
                .message("퇴근 처리 완료")
                .data(clockOut)
                .build());
    }

    //  기간별 근태 조회
    @Operation(summary = "기간별 근태 내역 조회", description = "사원 ID와 기간(시작일~종료일)을 기준으로 근태 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기간별 근태 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/attendanceslist/{employeeId}")
    public ResponseEntity<APIResponseDTO<List<AttendanceResponseDTO>>> getMyAttendanceList(
            @Parameter(description = "조회할 사원 ID", required = true, example = "1")
            @PathVariable Long employeeId,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", required = true, example = "2025-10-01")
            @RequestParam LocalDate start,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", required = true, example = "2025-10-31")
            @RequestParam LocalDate end
    ) {
        List<AttendanceResponseDTO> attendanceList = attendanceService.getMyAttendanceList(employeeId, start, end);
        return ResponseEntity.ok(APIResponseDTO.<List<AttendanceResponseDTO>>builder()
                .message("기간별 근태 내역 조회 성공")
                .data(attendanceList)
                .build());
    }

    //  전체 근태 조회 (관리자용)
    @Operation(summary = "전체 근태 내역 조회 (관리자)", description = "모든 사원의 근태 내역을 조회합니다. 관리자 권한이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전체 근태 내역 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content)
    })
    @GetMapping("/allattendances")
    public ResponseEntity<APIResponseDTO<List<AttendanceResponseDTO>>> getAllAttendances() {
        List<AttendanceResponseDTO> allAttendances = attendanceService.getAllAttendances();
        return ResponseEntity.ok(APIResponseDTO.<List<AttendanceResponseDTO>>builder()
                .message("전체 근태 내역 조회 성공")
                .data(allAttendances)
                .build());
    }

    //  외근 시작
    @Operation(summary = "외근 시작", description = "사원의 근무상태를 외근(OUT_ON_BUSINESS)으로 변경하고 이력을 기록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "외근 상태로 변경 완료"),
            @ApiResponse(responseCode = "400", description = "상태 변경 실패", content = @Content)
    })
    @PutMapping("/{employeeId}/out-on-business")
    public ResponseEntity<APIResponseDTO<String>> startOutOnBusiness(
            @Parameter(description = "사원 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        attendanceService.updateWorkStatus(employeeId, "OUT_ON_BUSINESS");
        return ResponseEntity.ok(APIResponseDTO.<String>builder()
                .message("외근 상태로 변경되었습니다.")
                .data("OUT_ON_BUSINESS")
                .build());
    }

    //  사내 복귀
    @Operation(summary = "사내 복귀", description = "외근 중인 사원의 근무상태를 이전 근무 상태 변경하고, 필요 시 이력을 기록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사내 복귀 완료"),
            @ApiResponse(responseCode = "400", description = "복귀 처리 실패", content = @Content)
    })
    @PutMapping("/{employeeId}/return-to-office")
    public ResponseEntity<APIResponseDTO<String>> returnToOffice(
            @Parameter(description = "사원 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        String finalStatus = attendanceService.updateWorkStatus(employeeId, "NORMAL");
        return ResponseEntity.ok(APIResponseDTO.<String>builder()
                .message("사내 복귀 완료")
                .data(finalStatus)
                .build());
    }

    //  근무 이력 조회
    @Operation(summary = "근무 이력 조회", description = "사원의 근무 상태 변경 이력을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "근무 이력 조회 성공"),
            @ApiResponse(responseCode = "404", description = "근무 이력이 존재하지 않음", content = @Content)
    })
    @GetMapping("/{employeeId}/work-history")
    public ResponseEntity<APIResponseDTO<List<AttendanceWorkHistoryResponseDTO>>> getWorkHistory(
            @Parameter(description = "사원 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        List<AttendanceWorkHistoryResponseDTO> historyList = attendanceWorkHistoryService
                .getHistoryByEmployee(employeeId)
                .stream()
                .map(AttendanceWorkHistory::toResponseDTO)
                .toList();

        return ResponseEntity.ok(APIResponseDTO.<List<AttendanceWorkHistoryResponseDTO>>builder()
                .message("근무 이력 조회 성공")
                .data(historyList)
                .build());
    }
    //  주간 근무시간 조회
    @Operation(summary = "주간 근무시간 조회", description = "지정한 주(또는 이번 주)의 누적 근무시간 및 목표 근로시간을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주간 근무시간 조회 성공"),
            @ApiResponse(responseCode = "400", description = "조회 실패", content = @Content)
    })
    @GetMapping("/weekly/{employeeId}")
    public ResponseEntity<APIResponseDTO<Map<String, Object>>> getWeeklyWorkSummary(
            @Parameter(description = "사원 ID", required = true, example = "1")
            @PathVariable Long employeeId,
            @Parameter(description = "조회할 주의 시작일(월요일, yyyy-MM-dd)", required = false, example = "2025-11-03")
            @RequestParam(required = false) String weekStart
    ) {
        Map<String, Object> result;

        if (weekStart != null && !weekStart.trim().isEmpty()) {
            try {
                LocalDate parsedStart = LocalDate.parse(weekStart);
                result = attendanceService.getWeeklyWorkSummary(employeeId, parsedStart);
            } catch (Exception e) {
                result = attendanceService.getWeeklyWorkSummary(employeeId); // fallback
            }
        } else {
            result = attendanceService.getWeeklyWorkSummary(employeeId);
        }

        return ResponseEntity.ok(APIResponseDTO.<Map<String, Object>>builder()
                .message("주간 근무 조회 성공")
                .data(result)
                .build());
    }


    //  근태 요약 조회
    @Operation(summary = "근태 요약 조회", description = "사원 ID를 기준으로 전체 근무일수, 총 근무시간, 잔여 연차, 이번 주 지각 횟수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "근태 요약 조회 성공"),
            @ApiResponse(responseCode = "404", description = "근태 요약 데이터 없음", content = @Content)
    })
    @GetMapping("/summary/{employeeId}")
    public ResponseEntity<APIResponseDTO<Map<String, Object>>> getAttendanceSummary(
            @Parameter(description = "사원 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        Map<String, Object> summary = attendanceService.getAttendanceSummary(employeeId);

        return ResponseEntity.ok(
                APIResponseDTO.<Map<String, Object>>builder()
                        .message("근태 요약 조회 성공")
                        .data(summary)
                        .build()
        );
    }

    //  결근 조회
    @Operation(
            summary = "결근 날짜 조회",
            description = "사원 ID와 연/월을 기준으로 해당 월의 결근 날짜 목록을 조회합니다. " +
                    "주말, 공휴일, 휴가(반차 포함)는 자동 제외됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결근 날짜 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/{employeeId}/absent")
    public ResponseEntity<APIResponseDTO<Map<String, Object>>> getAbsentDays(
            @Parameter(description = "사원 ID", required = true, example = "1")
            @PathVariable Long employeeId,

            @Parameter(description = "조회 연도", required = true, example = "2025")
            @RequestParam int year,

            @Parameter(description = "조회 월 (1~12)", required = true, example = "11")
            @RequestParam int month
    ) {
        List<LocalDate> absentDays = attendanceService.getAbsentDays(employeeId, year, month);

        Map<String, Object> data = Map.of("absentDays", absentDays);

        return ResponseEntity.ok(
                APIResponseDTO.<Map<String, Object>>builder()
                        .message("결근 날짜 조회 성공")
                        .data(data)
                        .build()
        );
    }
}