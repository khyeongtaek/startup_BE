package org.goodee.startup_BE.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.service.AttendanceService;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Attendance API", description = "근태 관리 API")
@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

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
}