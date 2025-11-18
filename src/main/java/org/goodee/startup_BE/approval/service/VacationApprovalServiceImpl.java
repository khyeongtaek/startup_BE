package org.goodee.startup_BE.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.repository.ApprovalDocRepository;
import org.goodee.startup_BE.attendance.service.AnnualLeaveService;
import org.goodee.startup_BE.attendance.service.AttendanceService;
import org.goodee.startup_BE.schedule.enums.ScheduleCategory;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.service.ScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

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

        // 2) 날짜 및 일수 가져오기 (엔티티에 이미 저장되어 있음)
        LocalDate startDate = doc.getStartDate().toLocalDate();
        LocalDate endDate = doc.getEndDate().toLocalDate();
        long vacationDays = doc.getVacationDays();  // Integer → long 자동 변환

        // 3) 연차 차감
        annualLeaveService.useAnnualLeave(employeeId, vacationDays);

        // 4) 근태 VACATION 처리 (기간 전체)
        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            attendanceService.markVacation(employeeId, d);
            d = d.plusDays(1);
        }

        // 5) 일정 자동 생성
        scheduleService.createSchedule(
                ScheduleRequestDTO.builder()
                        .employeeId(employeeId)
                        .title("휴가")
                        .categoryCode(ScheduleCategory.VACATION.name())
                        .startTime(startDate.atStartOfDay())
                        .endTime(endDate.atTime(23, 59, 59))
                        .content("휴가 일정(자동등록)")
                        .build()
        );
    }}