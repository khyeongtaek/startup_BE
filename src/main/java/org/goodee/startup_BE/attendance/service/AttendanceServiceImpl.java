package org.goodee.startup_BE.attendance.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.attendance.exception.AttendanceException;
import org.goodee.startup_BE.attendance.exception.DuplicateAttendanceException;
import org.goodee.startup_BE.attendance.repository.AttendanceRepository;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final AnnualLeaveService annualLeaveService;

    // 오늘 출근 기록 조회
    @Override
    @Transactional(readOnly = true)
    public AttendanceResponseDTO getTodayAttendance(Long employeeId) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository
                .findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new AttendanceException("오늘 출근 기록이 없습니다."));

        return AttendanceResponseDTO.builder()
                .attendanceId(attendance.getAttendanceId())
                .employeeId(attendance.getEmployee().getEmployeeId())
                .employeeName(attendance.getEmployee().getName())
                .attendanceDate(attendance.getAttendanceDate())
                .workDate(attendance.getWorkDate())
                .startTime(attendance.getStartTime())
                .endTime(attendance.getEndTime())
                .workStatus(attendance.getWorkStatus().getValue1())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }

    // 출근 등록
    @Override
    public AttendanceResponseDTO clockIn(Long employeeId) {
        LocalDate today = LocalDate.now();

        // 이미 출근한 경우 예외
        if (attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today).isPresent()) {
            throw new DuplicateAttendanceException("출근 기록이 이미 존재합니다.");
        }

        // 직원 조회
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        // 연차 자동생성
        annualLeaveService.createIfNotExists(employeeId);


        // 근무 상태 코드 NORMAL 조회
        List<CommonCode> codes = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues("WS", "NORMAL");
        if (codes.isEmpty()) {
            throw new AttendanceException("근무 상태 코드 'NORMAL'을 찾을 수 없습니다.");
        }

        CommonCode workStatus = codes.get(0);


        Integer workCount = attendanceRepository.countByEmployeeEmployeeId(employeeId) + 1;

        // 출근 기록 생성
        Attendance attendance = Attendance.createAttendance(employee, today, workStatus);
        attendance.setWorkDate(workCount);
        attendance.update(LocalDateTime.now(), null);  // 출근 시간 기록

        Attendance saved = attendanceRepository.save(attendance);

        return AttendanceResponseDTO.builder()
                .attendanceId(saved.getAttendanceId())
                .employeeId(saved.getEmployee().getEmployeeId())
                .employeeName(saved.getEmployee().getName())
                .attendanceDate(saved.getAttendanceDate())
                .workDate(saved.getWorkDate())
                .startTime(saved.getStartTime())
                .workStatus(saved.getWorkStatus().getValue1())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    // 퇴근 등록
    @Override
    public AttendanceResponseDTO clockOut(Long employeeId) {
        Attendance attendance = attendanceRepository.findCurrentWorkingRecord(employeeId)
                .orElseThrow(() -> new IllegalStateException("출근 기록이 없습니다."));

        // 근무 상태 코드 CLOCK_OUT 조회
        List<CommonCode> codes = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues("WS", "CLOCK_OUT");
        if (codes.isEmpty()) {
            throw new AttendanceException("근무 상태 코드 'CLOCK_OUT'을 찾을 수 없습니다.");
        }

        CommonCode workStatus = codes.get(0);

        //  이미 퇴근한 기록 방지
        if (attendance.getEndTime() != null) {
            throw new IllegalStateException("이미 퇴근 처리가 완료된 상태입니다.");
        }

        // 퇴근 시간 업데이트
        attendance.changeWorkStatus(workStatus);
        attendance.update(attendance.getStartTime(), LocalDateTime.now());
        Attendance saved = attendanceRepository.save(attendance);

        return AttendanceResponseDTO.builder()
                .attendanceId(saved.getAttendanceId())
                .employeeId(saved.getEmployee().getEmployeeId())
                .employeeName(saved.getEmployee().getName())
                .attendanceDate(saved.getAttendanceDate())
                .workDate(saved.getWorkDate())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .workStatus(saved.getWorkStatus().getValue1())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    // 내 근태 내역 조회 (기간)
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getMyAttendanceList(Long employeeId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDateBetween(employeeId, start, end)
                .stream()
                .map(a -> AttendanceResponseDTO.builder()
                        .attendanceId(a.getAttendanceId())
                        .employeeId(a.getEmployee().getEmployeeId())
                        .employeeName(a.getEmployee().getName())
                        .attendanceDate(a.getAttendanceDate())
                        .workDate(a.getWorkDate())
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .workStatus(a.getWorkStatus().getValue1())
                        .createdAt(a.getCreatedAt())
                        .updatedAt(a.getUpdatedAt())
                        .build())
                .toList();
    }

    // 관리자 전체 근태 조회
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getAllAttendances() {
        return attendanceRepository.findByIsDeletedIsFalse()
                .stream()
                .map(a -> AttendanceResponseDTO.builder()
                        .attendanceId(a.getAttendanceId())
                        .employeeId(a.getEmployee().getEmployeeId())
                        .employeeName(a.getEmployee().getName())
                        .attendanceDate(a.getAttendanceDate())
                        .workDate(a.getWorkDate())
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .workStatus(a.getWorkStatus().getValue1())
                        .createdAt(a.getCreatedAt())
                        .updatedAt(a.getUpdatedAt())
                        .build())
                .toList();
    }
}