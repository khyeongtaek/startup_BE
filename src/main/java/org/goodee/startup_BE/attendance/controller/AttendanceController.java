package org.goodee.startup_BE.attendance.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.service.AttendanceService;
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

    // 본인 출근 기록 조회
    @GetMapping("/today/{employeeId}")
    public ResponseEntity<AttendanceResponseDTO> getTodayAttendance(@PathVariable Long employeeId) {
        AttendanceResponseDTO getAttendance = attendanceService.getTodayAttendance(employeeId);
        return ResponseEntity.ok(getAttendance);
    }

    // 출근
    @PostMapping("/clock-in/{employeeId}")
    public ResponseEntity<AttendanceResponseDTO> clockIn(@PathVariable Long employeeId) {
        AttendanceResponseDTO clockIn = attendanceService.clockIn(employeeId);
        return ResponseEntity.ok(clockIn);
    }

    // 퇴근
    @PostMapping("/clock-out/{employeeId}")
    public ResponseEntity<AttendanceResponseDTO> clockOut(@PathVariable Long employeeId) {
        AttendanceResponseDTO clockOut = attendanceService.clockOut(employeeId);
        return ResponseEntity.ok(clockOut);
    }

    // 기간별 근태 내역 조회
    @GetMapping("/attendanceslist/{employeeId}")
    public ResponseEntity<List<AttendanceResponseDTO>> getMyAttendanceList (
            @PathVariable Long employeeId,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
            ){
        List<AttendanceResponseDTO> attendanceList = attendanceService.getMyAttendanceList(employeeId,start,end);
        return ResponseEntity.ok(attendanceList);
    }

    // 관리자 전제 근태 조회
    @GetMapping("/allattendances")
    public ResponseEntity<List<AttendanceResponseDTO>> getAllAttendances(){
        List<AttendanceResponseDTO> allAttendances = attendanceService.getAllAttendances();
        return ResponseEntity.ok(allAttendances);
    }
}
