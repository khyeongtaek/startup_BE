package org.goodee.startup_BE.approval.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.enums.VacationType;
import org.goodee.startup_BE.approval.repository.ApprovalDocRepository;
import org.goodee.startup_BE.attendance.service.AnnualLeaveService;
import org.goodee.startup_BE.attendance.service.AttendanceService;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.enums.ScheduleCategory;
import org.goodee.startup_BE.schedule.service.ScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VacationApprovalServiceImpl implements VacationApprovalService {

    private final ApprovalDocRepository approvalDocRepository;
    private final AnnualLeaveService annualLeaveService;
    private final AttendanceService attendanceService;
    private final ScheduleService scheduleService;

    @Override
    public void handleApprovedVacation(Long docId) {

        // 1) 문서 조회
        ApprovalDoc doc = approvalDocRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("결재 문서를 찾을 수 없습니다."));

        // 기안자 ID
        Long employeeId = doc.getCreator().getEmployeeId();

        // 2) 날짜 및 일수 가져오기
        LocalDate startDate = doc.getStartDate().toLocalDate();
        LocalDate endDate = doc.getEndDate().toLocalDate();
        Double vacationDays = doc.getVacationDays();

        // 3) 연차 차감
        annualLeaveService.useAnnualLeave(employeeId, vacationDays);

        // 4) 근태 VACATION / 반차 처리
        String vacationTypeRaw = (doc.getVacationType() != null && doc.getVacationType().getValue1() != null)
                ? doc.getVacationType().getValue1()
                : "ANNUAL";

        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            attendanceService.markVacation(employeeId, d, vacationTypeRaw);
            d = d.plusDays(1);
        }

        // ============================================================
        // 5) 일정 자동 생성
        // ============================================================

        // VacationType enum 변환
        VacationType vacationType;
        try {
            vacationType = VacationType.valueOf(vacationTypeRaw);
        } catch (Exception e) {
            vacationType = VacationType.ANNUAL;
        }

        // 정책 기반 시간 정의
        final LocalTime MORNING_HALF_START = LocalTime.of(9, 0);
        final LocalTime MORNING_HALF_END   = LocalTime.of(14, 0);

        final LocalTime AFTERNOON_HALF_START = LocalTime.of(14, 0);
        final LocalTime AFTERNOON_HALF_END   = LocalTime.of(18, 0);

        final LocalTime FULL_DAY_START = LocalTime.of(9, 0);
        final LocalTime FULL_DAY_END   = LocalTime.of(18, 0);

        // 일정 생성 변수
        String title;
        LocalDateTime scheduleStart;
        LocalDateTime scheduleEnd;

        switch (vacationType) {
            case MORNING_HALF:
                title = "오전 반차";
                scheduleStart = startDate.atTime(MORNING_HALF_START);
                scheduleEnd = startDate.atTime(MORNING_HALF_END);
                break;

            case AFTERNOON_HALF:
                title = "오후 반차";
                scheduleStart = startDate.atTime(AFTERNOON_HALF_START);
                scheduleEnd = startDate.atTime(AFTERNOON_HALF_END);
                break;

            case ANNUAL:
            default:
                title = "휴가";
                scheduleStart = startDate.atTime(FULL_DAY_START);
                scheduleEnd = endDate.atTime(FULL_DAY_END);
                break;
        }

        // 일정 생성
        scheduleService.createSchedule(
                ScheduleRequestDTO.builder()
                        .employeeId(employeeId)
                        .title(title)
                        .categoryCode(ScheduleCategory.VACATION.name())
                        .startTime(scheduleStart)
                        .endTime(scheduleEnd)
                        .content(title + " 일정(자동등록)")
                        .build()
        );
    }
}