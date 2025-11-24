package org.goodee.startup_BE.approval.service;

import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.repository.ApprovalDocRepository;
import org.goodee.startup_BE.attendance.service.AnnualLeaveService;
import org.goodee.startup_BE.attendance.service.AttendanceService;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.enums.ScheduleCategory;
import org.goodee.startup_BE.schedule.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class VacationApprovalServiceImplTest {

    @InjectMocks
    private VacationApprovalServiceImpl vacationApprovalService;

    @Mock
    private ApprovalDocRepository approvalDocRepository;

    @Mock
    private AnnualLeaveService annualLeaveService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private ScheduleService scheduleService;

    // --- 공용 Mock 엔티티 ---
    private ApprovalDoc mockDoc;
    private Employee mockCreator;

    @BeforeEach
    void setUp() {
        mockDoc = mock(ApprovalDoc.class);
        mockCreator = mock(Employee.class);

        // ApprovalDoc → Creator
        lenient().when(mockDoc.getCreator()).thenReturn(mockCreator);

        // Creator 공용 stubbing
        lenient().when(mockCreator.getEmployeeId()).thenReturn(10L);
        lenient().when(mockCreator.getName()).thenReturn("홍길동");
        lenient().when(mockCreator.getUsername()).thenReturn("creator");
    }

    @Test
    @DisplayName("handleApprovedVacation - 성공 (연차 차감, 근태 VACATION 처리, 일정 생성)")
    void handleApprovedVacation_Success() {

        Long docId = 1L;
        Long employeeId = 10L;

        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 3);

        // ApprovalDoc 필드 Stub
        given(mockDoc.getStartDate()).willReturn(start.atStartOfDay());
        given(mockDoc.getEndDate()).willReturn(end.atTime(23, 59, 59));
        given(mockDoc.getVacationDays()).willReturn(3.0);
        given(mockDoc.getCreator()).willReturn(mockCreator);

        // Repository Stub
        given(approvalDocRepository.findById(docId)).willReturn(Optional.of(mockDoc));

        // when
        vacationApprovalService.handleApprovedVacation(docId);

        // then
        // 1) 연차 차감
        then(annualLeaveService).should(times(1))
                .useAnnualLeave(employeeId, 3.0);

        // 2) 매일 근태 VACATION
        then(attendanceService).should(times(3))
                .markVacation(eq(employeeId), any(LocalDate.class), eq("ANNUAL"));

        // 3) 일정 생성
        then(scheduleService).should(times(1))
                .createSchedule(any(ScheduleRequestDTO.class));
    }


    @Test
    @DisplayName("handleApprovedVacation - 실패 (문서 없음)")
    void handleApprovedVacation_Fail_NotFound() {

        Long docId = 999L;

        given(approvalDocRepository.findById(docId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> vacationApprovalService.handleApprovedVacation(docId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결재 문서를 찾을 수 없습니다.");
    }
}