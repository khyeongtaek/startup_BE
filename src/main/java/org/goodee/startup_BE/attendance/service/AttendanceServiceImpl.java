package org.goodee.startup_BE.attendance.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.attendance.dto.AttendanceResponseDTO;
import org.goodee.startup_BE.attendance.entity.AnnualLeave;
import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.attendance.entity.AttendanceWorkHistory;
import org.goodee.startup_BE.attendance.enums.WorkStatus;
import org.goodee.startup_BE.attendance.exception.AttendanceException;
import org.goodee.startup_BE.attendance.exception.DuplicateAttendanceException;
import org.goodee.startup_BE.attendance.repository.AttendanceRepository;
import org.goodee.startup_BE.attendance.repository.AttendanceWorkHistoryRepository;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final AnnualLeaveService annualLeaveService;
    private final AttendanceWorkHistoryService attendanceWorkHistoryService;
    private final AttendanceWorkHistoryRepository historyRepository;

    // 공통 코드 Prefix 정의
    private static final String WOKR_STATUS_PREFIX = WorkStatus.PREFIX;

    // Value1 정의 (근무 상태)
    private static final String WORK_STATUS_NORMAL = WorkStatus.NORMAL.name();
    private static final String WORK_STATUS_LATE = WorkStatus.LATE.name();
    private static final String WORK_STATUS_EARLY_LEAVE = WorkStatus.EARLY_LEAVE.name();
    private static final String WORK_STATUS_ABSENT = WorkStatus.ABSENT.name();
    private static final String WORK_STATUS_VACATION = WorkStatus.VACATION.name();
    private static final String WORK_STATUS_OUT_ON_BUSINESS = WorkStatus.OUT_ON_BUSINESS.name();
    private static final String WORK_STATUS_CLOCK_OUT = WorkStatus.CLOCK_OUT.name();
    private static final String WORK_STATUS_MORNING_HALF = WorkStatus.MORNING_HALF.name();
    private static final String WORK_STATUS_AFTERNOON_HALF = WorkStatus.AFTERNOON_HALF.name();

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
        LocalDateTime now = LocalDateTime.now();

        // 오늘 기록 여부 확인
        Attendance attendance = attendanceRepository
                .findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today)
                .orElse(null);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        // 연차 자동 생성 (단 출근 기록 새로 만들 경우에만)
        annualLeaveService.createIfNotExists(employeeId);

        // ============================================
        // 1) 이미 Attendance가 존재하는 경우 (휴가/반차 포함)
        // ============================================
        if (attendance != null) {

            // 이미 출근했다면 예외
            if (attendance.getStartTime() != null) {
                throw new DuplicateAttendanceException("이미 출근 기록이 존재합니다.");
            }

            String status = attendance.getWorkStatus().getValue1();

            // ---- 반차 근무 제한 ----
            if (WORK_STATUS_MORNING_HALF.equals(status)) {
                if (now.toLocalTime().isBefore(LocalTime.of(13, 0))) {
                    throw new AttendanceException("오전 반차 사용자는 오후 1시 이후 출근 가능합니다.");
                }
            }

            if (WORK_STATUS_AFTERNOON_HALF.equals(status)) {
                if (now.toLocalTime().isAfter(LocalTime.of(14, 0))) {
                    throw new AttendanceException("오후 반차 사용자는 오전 근무만 가능합니다.");
                }
            }

        } else {
            // ============================================
            // 2) Attendance가 없는 경우 → NORMAL로 새로 생성
            // ============================================
            CommonCode normalCode = getCommonCode(WOKR_STATUS_PREFIX, WORK_STATUS_NORMAL);

            attendance = Attendance.createAttendance(employee, today, normalCode);
            attendance.setWorkDate(attendanceRepository.countByEmployeeEmployeeId(employeeId) + 1);
        }

        // ============================================
        // 3) 출근 시간 기록
        // ============================================
        attendance.update(now, null);

        // ============================================
        // 4) 지각 판정 (반차 여부에 따라 기준 시간 다름)
        // ============================================
        String status = attendance.getWorkStatus().getValue1();
        LocalTime lateStandardTime;

        if (WORK_STATUS_MORNING_HALF.equals(status)) {
            // 오전 반차 → 오후 1시 이후부터 지각
            lateStandardTime = LocalTime.of(14, 0);
        } else if (WORK_STATUS_AFTERNOON_HALF.equals(status)) {
            // 오후 반차 → 오전 9시 기준 (오후 반차자는 오전만 근무)
            lateStandardTime = LocalTime.of(9, 0);
        } else {
            // 일반 근무
            lateStandardTime = LocalTime.of(9, 0);
        }

        if (now.toLocalTime().isAfter(lateStandardTime)) {
            attendance.changeWorkStatus(getCommonCode(WOKR_STATUS_PREFIX, WORK_STATUS_LATE));
            log.info("[지각] {}님이 {}에 출근했습니다.", employee.getName(), now.toLocalTime());
        } else {
            log.info("[정상 출근] {}님이 {}에 출근했습니다.", employee.getName(), now.toLocalTime());
        }

        Attendance saved = attendanceRepository.save(attendance);

        // ============================================
        // 5) 이력 기록
        // ============================================
        attendanceWorkHistoryService.recordHistory(saved, employee, saved.getWorkStatus().getValue1());

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
                .findByCodeStartsWithAndKeywordExactMatchInValues(WorkStatus.PREFIX, WORK_STATUS_CLOCK_OUT);
        if (codes.isEmpty()) {
            throw new AttendanceException("근무 상태 코드 'CLOCK_OUT'을 찾을 수 없습니다.");
        }

        CommonCode workStatus = codes.get(0);

        // 이미 퇴근한 기록 방지
        if (attendance.getEndTime() != null) {
            throw new IllegalStateException("이미 퇴근 처리가 완료된 상태입니다.");
        }

        // 퇴근 시간 업데이트
        attendance.changeWorkStatus(workStatus);
        attendance.update(attendance.getStartTime(), LocalDateTime.now());

        LocalDateTime endTime = LocalDateTime.now();
        attendance.update(attendance.getStartTime(), endTime);

        // 조퇴 판정
        if (endTime.toLocalTime().isBefore(LocalTime.of(18, 0))) {
            attendance.changeWorkStatus(getCommonCode(WOKR_STATUS_PREFIX, WORK_STATUS_EARLY_LEAVE));
            log.info("[조퇴] {}님이 {}에 퇴근했습니다.", attendance.getEmployee().getName(), endTime.toLocalTime());
        } else {
            attendance.changeWorkStatus(getCommonCode(WOKR_STATUS_PREFIX, WORK_STATUS_CLOCK_OUT));
            log.info("[정상 퇴근] {}님이 {}에 퇴근했습니다.", attendance.getEmployee().getName(), endTime.toLocalTime());
        }
        Attendance saved = attendanceRepository.save(attendance);

        attendanceWorkHistoryService.recordHistory(saved, saved.getEmployee(), saved.getWorkStatus().getValue1());

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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getWeeklyWorkSummary(Long employeeId, LocalDate weekStart) {
        // weekStart가 없으면 이번 주 월요일
        LocalDate startOfWeek = (weekStart != null)
                ? weekStart
                : LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<Attendance> weeklyRecords = attendanceRepository.findWeeklyRecords(employeeId, startOfWeek, endOfWeek);

        long totalMinutes = weeklyRecords.stream()
                .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                .mapToLong(a -> Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                .sum();

        long targetMinutes = 40 * 60;

        Map<String, Object> result = new HashMap<>();
        result.put("records", weeklyRecords.stream()
                .map(a -> AttendanceResponseDTO.builder()
                        .attendanceId(a.getAttendanceId())
                        .employeeId(a.getEmployee().getEmployeeId())
                        .employeeName(a.getEmployee().getName())
                        .attendanceDate(a.getAttendanceDate())
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .workStatus(a.getWorkStatus().getValue1())
                        .build())
                .toList());
        result.put("totalMinutes", totalMinutes);
        result.put("targetMinutes", targetMinutes);
        result.put("totalHours", totalMinutes / 60);
        result.put("targetHours", targetMinutes / 60);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getWeeklyWorkSummary(Long employeeId) {
        // 이번 주 월요일 ~ 일요일 계산
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        // 이번 주 출퇴근 데이터 조회
        List<Attendance> weeklyRecords = attendanceRepository.findWeeklyRecords(employeeId, startOfWeek, endOfWeek);

        // 총 근무시간(분 단위)
        long totalMinutes = weeklyRecords.stream()
                .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                .mapToLong(a -> Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                .sum();

        // 목표 근무시간 (기본 40시간)
        long targetMinutes = 40 * 60;

        List<Map<String, Object>> recordList = weeklyRecords.stream()
                .map(a -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("attendanceDate", a.getAttendanceDate());
                    record.put("startTime", a.getStartTime());
                    record.put("endTime", a.getEndTime());
                    record.put("workStatus", a.getWorkStatus().getValue1());
                    return record;
                })
                .toList();

        // 응답 데이터 구성
        Map<String, Object> result = new HashMap<>();
        result.put("records", recordList);
        result.put("totalMinutes", totalMinutes);
        result.put("targetMinutes", targetMinutes);
        result.put("totalHours", totalMinutes / 60);
        result.put("targetHours", targetMinutes / 60);

        return result;
    }

    @Override
    @Transactional
    public String updateWorkStatus(Long employeeId, String statusCode) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository
                .findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new ResourceNotFoundException("오늘 출근 기록이 존재하지 않습니다."));

        String finalStatusValue = statusCode;

        // 복귀: OUT_ON_BUSINESS 직전 "같은 Attendance"의 상태로 복원
        if (WORK_STATUS_NORMAL.equals(statusCode)) {
            // 오늘 출근건 (history는 같은 attendance_id 기준)
            List<AttendanceWorkHistory> histories =
                    historyRepository.findByAttendanceAttendanceIdOrderByActionTimeDesc(attendance.getAttendanceId());

            // histories[0] = OUT_ON_BUSINESS 이어야 정상. 그 이전 “비-외근 상태”를 찾는다.
            finalStatusValue = histories.stream()
                    .map(h -> h.getActionCode().getValue1())
                    // 첫 번째 OUT_ON_BUSINESS는 건너뛰고
                    .skip(1)
                    // OUT_ON_BUSINESS가 아닌 첫 번째 상태(예: LATE, NORMAL 등)
                    .filter(v -> !WORK_STATUS_OUT_ON_BUSINESS.equals(v))
                    .findFirst()
                    // 못 찾으면 NORMAL로 폴백
                    .orElse(WORK_STATUS_NORMAL);
        }

        // 최종 상태 코드로 CommonCode 조회
        CommonCode newStatus = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues(WorkStatus.PREFIX, finalStatusValue)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AttendanceException("해당 근무 상태 코드를 찾을 수 없습니다."));

        attendance.changeWorkStatus(newStatus);
        attendanceRepository.save(attendance);

        // 이력 기록
        attendanceWorkHistoryService.recordHistory(attendance, attendance.getEmployee(), newStatus.getValue1());
        return newStatus.getValue1();
    }

    /**
     * 공통 코드 조회
     *
     * @param codePrefix (예: "AD", "AL")
     * @param value1     (예: "IN_PROGRESS", "PENDING")
     * @return CommonCode 엔티티
     */
    private CommonCode getCommonCode(String codePrefix, String value1) {
        try {
            List<CommonCode> codes = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(
                    codePrefix,
                    value1
            );
            if (codes.isEmpty()) {
                throw new EntityNotFoundException("공통 코드를 찾을 수 없습니다: " + codePrefix + ", " + value1);
            }
            return codes.get(0);
        } catch (Exception e) {
            log.error("공통 코드 조회 중 오류 발생: {} / {}", codePrefix, value1, e);
            throw new EntityNotFoundException("공통 코드 조회 실패: " + codePrefix + ", " + value1);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAttendanceSummary(Long employeeId) {

        // (1) 전체 근무일수
        Long totalDays = attendanceRepository.countByEmployeeEmployeeId(employeeId);
        if (totalDays == null) totalDays = 0L;

        // (2) 전체 근무시간 (출근~퇴근 시간 합계)
        List<Attendance> allRecords = attendanceRepository.findByEmployeeEmployeeId(employeeId);
        Long totalMinutes = allRecords.stream()
                .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                .mapToLong(a -> Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                .sum();

        Long totalHours = totalMinutes / 60;

        // (3) 잔여 연차
        AnnualLeave leave = annualLeaveService.getAnnualLeave(employeeId);
        Double remainingLeave = 0.0;
        if (leave != null && leave.getRemainingDays() != null) {
            remainingLeave = leave.getRemainingDays();
        }

        // (4) 이번 주 지각 횟수
        LocalDate startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = LocalDate.now().with(DayOfWeek.SUNDAY);
        Long lateCount = attendanceRepository.countLatesThisWeek(employeeId, startOfWeek, endOfWeek);
        if (lateCount == null) lateCount = 0L;

        // (5) 결과 맵 구성
        Map<String, Object> result = new HashMap<>();
        result.put("totalDays", totalDays);
        result.put("totalHours", totalHours);
        result.put("totalMinutes", totalMinutes);
        result.put("remainingLeave", remainingLeave);
        result.put("lateCount", lateCount);

        return result;
    }

    // 🔥 휴가 / 반차 등록: VacationType(ANNUAL / MORNING_HALF / AFTERNOON_HALF)에 따라 상태 코드 다르게 반영
    @Override
    @Transactional
    public void markVacation(Long employeeId, LocalDate date, String vacationType) {

        // 1) 해당 날짜 Attendance 조회 or 새로 생성
        Attendance attendance = attendanceRepository
                .findByEmployeeEmployeeIdAndAttendanceDate(employeeId, date)
                .orElseGet(() -> {
                    Employee employee = employeeRepository.findById(employeeId)
                            .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));
                    CommonCode defaultStatus = getCommonCode(WorkStatus.PREFIX, WORK_STATUS_VACATION);
                    Attendance newA = Attendance.createAttendance(employee, date, defaultStatus);
                    return attendanceRepository.save(newA);
                });

        // 2) vacationType(문자열) → WorkStatus value1 매핑
        String statusValue = switch (vacationType) {
            case "MORNING_HALF" -> WORK_STATUS_MORNING_HALF;
            case "AFTERNOON_HALF" -> WORK_STATUS_AFTERNOON_HALF;
            default -> WORK_STATUS_VACATION; // ANNUAL 또는 기타는 전부 '휴가' 처리
        };

        // 3) 해당 WorkStatus 코드 조회
        CommonCode statusCode = getCommonCode(WorkStatus.PREFIX, statusValue);

        // 4) 상태 변경 및 저장
        attendance.changeWorkStatus(statusCode);
        attendanceRepository.save(attendance);

        // 5) 이력 기록
        attendanceWorkHistoryService.recordHistory(attendance, attendance.getEmployee(), statusValue);
    }
}