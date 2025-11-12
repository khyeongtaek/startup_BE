package org.goodee.startup_BE.attendance.service;

import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.attendance.entity.AttendanceWorkHistory;
import org.goodee.startup_BE.attendance.repository.AttendanceWorkHistoryRepository;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceWorkHistoryServiceImplTest {

    @InjectMocks
    private AttendanceWorkHistoryServiceImpl attendanceWorkHistoryService;

    @Mock
    private AttendanceWorkHistoryRepository historyRepository;

    @Mock
    private CommonCodeRepository commonCodeRepository;

    private Employee mockEmployee;
    private Attendance mockAttendance;
    private CommonCode mockWorkStatusNormal;
    private CommonCode mockWorkStatusLate;

    @BeforeEach
    void setUp() {
        mockEmployee = mock(Employee.class);
        mockAttendance = mock(Attendance.class);
        mockWorkStatusNormal = mock(CommonCode.class);
        mockWorkStatusLate = mock(CommonCode.class);

        lenient().when(mockEmployee.getEmployeeId()).thenReturn(1L);
        lenient().when(mockEmployee.getName()).thenReturn("테스트 사원");
        lenient().when(mockAttendance.getAttendanceId()).thenReturn(100L);
        lenient().when(mockAttendance.getEmployee()).thenReturn(mockEmployee);
        lenient().when(mockWorkStatusNormal.getValue1()).thenReturn("NORMAL");
        lenient().when(mockWorkStatusLate.getValue1()).thenReturn("LATE");
    }

    // ===================== recordHistory() =====================
    @Nested
    @DisplayName("recordHistory() 근무 이력 기록")
    class RecordHistory {

        @Test
        @DisplayName("성공 - 근무 상태 코드가 존재할 때 이력 저장")
        void recordHistory_Success() {
            // given
            String actionCode = "NORMAL";
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", actionCode))
                    .willReturn(List.of(mockWorkStatusNormal));
            given(historyRepository.save(any(AttendanceWorkHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            attendanceWorkHistoryService.recordHistory(mockAttendance, mockEmployee, actionCode);

            // then
            ArgumentCaptor<AttendanceWorkHistory> captor =
                    ArgumentCaptor.forClass(AttendanceWorkHistory.class);
            verify(historyRepository, times(1)).save(captor.capture());

            AttendanceWorkHistory saved = captor.getValue();
            assertThat(saved.getAttendance()).isEqualTo(mockAttendance);
            assertThat(saved.getEmployee()).isEqualTo(mockEmployee);
            assertThat(saved.getActionCode().getValue1()).isEqualTo("NORMAL");
            assertThat(saved.getActionTime()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 근무 상태 코드일 때 예외 발생")
        void recordHistory_Fail_InvalidCode() {
            // given
            String invalidCode = "UNKNOWN";
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", invalidCode))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() ->
                    attendanceWorkHistoryService.recordHistory(mockAttendance, mockEmployee, invalidCode))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("유효하지 않은 근무 상태 코드입니다");
            verify(historyRepository, never()).save(any());
        }
    }

    // ===================== getHistoryByEmployee() =====================
    @Nested
    @DisplayName("getHistoryByEmployee() 사원별 근무 이력 조회")
    class GetHistoryByEmployee {

        @Test
        @DisplayName("성공 - 사원 ID 기준 내림차순 조회")
        void getHistoryByEmployee_Success() {
            // given
            AttendanceWorkHistory history1 = AttendanceWorkHistory.builder()
                    .attendance(mockAttendance)
                    .employee(mockEmployee)
                    .actionCode(mockWorkStatusNormal)
                    .actionTime(LocalDateTime.now().minusMinutes(3))
                    .build();

            AttendanceWorkHistory history2 = AttendanceWorkHistory.builder()
                    .attendance(mockAttendance)
                    .employee(mockEmployee)
                    .actionCode(mockWorkStatusLate)
                    .actionTime(LocalDateTime.now())
                    .build();

            given(historyRepository.findByEmployeeEmployeeIdOrderByActionTimeDesc(1L))
                    .willReturn(List.of(history2, history1));

            // when
            List<AttendanceWorkHistory> result =
                    attendanceWorkHistoryService.getHistoryByEmployee(1L);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getActionCode().getValue1()).isEqualTo("LATE");
            assertThat(result.get(1).getActionCode().getValue1()).isEqualTo("NORMAL");
            verify(historyRepository, times(1))
                    .findByEmployeeEmployeeIdOrderByActionTimeDesc(1L);
        }
    }
}