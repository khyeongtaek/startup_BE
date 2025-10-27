package org.goodee.startup_BE.attendance.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.attendance.repository.AttendanceRepository;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService{

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;


    // 오늘 출근 기록 조회
    @Override
    @Transactional(readOnly = true)
    public AttendanceResponseDTO getTodayAttendance(Long employeeId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> todaysAttendance =
                    attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today);
        return todaysAttendance.map(AttendanceResponseDTO :: toDTO).orElse(null);
    }

    // 출근 등록
    @Override
    public AttendanceResponseDTO clockIn(Long employeeId) {
        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today).isPresent()){
            throw new IllegalStateException("출근 기록이 이미 존재합니다.");
        }
        // 직원 정보 조회
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("직원 정보를 찾을 수 없습니다."));

        List<CommonCode> codes = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "ABSENT");

        if (codes.isEmpty()) {
            throw new IllegalArgumentException("근무 상태 코드 'ABSENT'를 찾을 수 없습니다.");
        }

        CommonCode workStatus = codes.get(0);

        // 출근 기록 생성
        Attendance attendance = Attendance.createAttendance(employee, today, workStatus);
        attendance.update(LocalDateTime.now(), null);  // 출근 시간 기록

        Attendance saved = attendanceRepository.save(attendance);
        return  AttendanceResponseDTO.toDTO(saved);
    }

    // 퇴근 등록
    @Override
    public AttendanceResponseDTO clockOut(Long employeeId) {
        Attendance attendance = attendanceRepository.findCurrentWorkingRecord(employeeId).orElseThrow(() -> new IllegalStateException("출근 기록이 없습니다."));

        List<CommonCode> codes = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "ABSENT");

        if (codes.isEmpty()) {
            throw new IllegalArgumentException("근무 상태 코드 'ABSENT'를 찾을 수 없습니다.");
        }

        CommonCode workStatus = codes.get(0);
        attendance.changeWorkStatus(workStatus);
        attendance.update(attendance.getStartTime(), LocalDateTime.now());
        Attendance saved = attendanceRepository.save(attendance);

        return AttendanceResponseDTO.toDTO(attendance);
    }

    // 내 근태 내역 조회 (기간)
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getMyAttendanceList(Long employeeId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDateBetween(employeeId, start, end)
                .stream()
                .map(AttendanceResponseDTO::toDTO)
                .toList();
    }
    // 관리자 전체 근태 조회
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getAllAttendances() {
        return attendanceRepository.findByIsDeletedIsFalse()
                .stream()
                .map(AttendanceResponseDTO:: toDTO)
                .toList();
    }

}
