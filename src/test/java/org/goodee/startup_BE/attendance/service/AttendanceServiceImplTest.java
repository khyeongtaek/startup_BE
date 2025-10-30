package org.goodee.startup_BE.attendance.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CommonCodeRepository commonCodeRepository;

    private Employee mockEmployee;
    private CommonCode mockWorkStatusNormal;
    private CommonCode mockWorkStatusOut;
    private Attendance mockAttendanceToday;

    @BeforeEach
    void setUp() {
        mockEmployee = mock(Employee.class);
        mockWorkStatusNormal = mock(CommonCode.class);
        mockWorkStatusOut = mock(CommonCode.class);
        mockAttendanceToday = mock(Attendance.class);

        lenient().when(mockEmployee.getEmployeeId()).thenReturn(1L);
        lenient().when(mockEmployee.getName()).thenReturn("테스트 사원");
        lenient().when(mockWorkStatusNormal.getValue1()).thenReturn("NORMAL");
        lenient().when(mockWorkStatusOut.getValue1()).thenReturn("CLOCK_OUT");
    }

    // ===================== 출근 테스트 =====================
    @Nested
    @DisplayName("clockIn() 출근 등록")
    class ClockIn {

        @Test
        @DisplayName("성공 - 정상 출근")
        void clockIn_Success() {
            // given
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();

            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.empty());
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "NORMAL"))
                    .willReturn(List.of(mockWorkStatusNormal));

            Attendance newAttendance = Attendance.createAttendance(mockEmployee, today, mockWorkStatusNormal);
            given(attendanceRepository.save(any(Attendance.class))).willReturn(newAttendance);

            // when
            AttendanceResponseDTO result = attendanceService.clockIn(employeeId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWorkStatus()).isEqualTo("NORMAL");
            verify(attendanceRepository, times(1)).save(any(Attendance.class));
        }

        @Test
        @DisplayName("실패 - 이미 출근 기록 존재")
        void clockIn_Fail_Duplicate() {
            // given
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();
            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.of(mockAttendanceToday));

            // when & then
            assertThatThrownBy(() -> attendanceService.clockIn(employeeId))
                    .isInstanceOf(DuplicateAttendanceException.class)
                    .hasMessageContaining("출근 기록이 이미 존재합니다");
        }

        @Test
        @DisplayName("실패 - 직원 정보 없음")
        void clockIn_Fail_NoEmployee() {
            // given
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();
            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.empty());
            given(employeeRepository.findById(employeeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> attendanceService.clockIn(employeeId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("사원 정보를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패 - NORMAL 코드 없음")
        void clockIn_Fail_NoCode() {
            // given
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();
            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.empty());
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "NORMAL"))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> attendanceService.clockIn(employeeId))
                    .isInstanceOf(AttendanceException.class)
                    .hasMessageContaining("근무 상태 코드 'NORMAL'을 찾을 수 없습니다");
        }
    }

    // ===================== 퇴근 테스트 =====================
    @Nested
    @DisplayName("clockOut() 퇴근 등록")
    class ClockOut {

        @Test
        @DisplayName("성공 - 정상 퇴근")
        void clockOut_Success() {
            // given
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();
            Attendance attendance = Attendance.createAttendance(mockEmployee, today, mockWorkStatusNormal);

            given(attendanceRepository.findCurrentWorkingRecord(employeeId))
                    .willReturn(Optional.of(attendance));
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "CLOCK_OUT"))
                    .willReturn(List.of(mockWorkStatusOut));
            given(attendanceRepository.save(any(Attendance.class))).willReturn(attendance);

            // when
            AttendanceResponseDTO result = attendanceService.clockOut(employeeId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWorkStatus()).isEqualTo("CLOCK_OUT");
            verify(attendanceRepository, times(1)).save(any(Attendance.class));
        }

        @Test
        @DisplayName("실패 - 출근 기록 없음")
        void clockOut_Fail_NoRecord() {
            // given
            Long employeeId = 1L;
            given(attendanceRepository.findCurrentWorkingRecord(employeeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> attendanceService.clockOut(employeeId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("출근 기록이 없습니다");
        }

        @Test
        @DisplayName("실패 - CLOCK_OUT 코드 없음")
        void clockOut_Fail_NoCode() {
            // given
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();
            Attendance attendance = Attendance.createAttendance(mockEmployee, today, mockWorkStatusNormal);

            given(attendanceRepository.findCurrentWorkingRecord(employeeId)).willReturn(Optional.of(attendance));
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "CLOCK_OUT"))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> attendanceService.clockOut(employeeId))
                    .isInstanceOf(AttendanceException.class)
                    .hasMessageContaining("근무 상태 코드");
        }
    }

    // ===================== 오늘 출근 조회 =====================
    @Nested
    @DisplayName("getTodayAttendance() 오늘 근태 조회")
    class GetTodayAttendance {

        @Test
        @DisplayName("성공 - 오늘 출근 기록 존재")
        void getTodayAttendance_Success() {
            // given
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();
            Attendance attendance = Attendance.createAttendance(mockEmployee, today, mockWorkStatusNormal);
            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.of(attendance));

            // when
            AttendanceResponseDTO result = attendanceService.getTodayAttendance(employeeId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWorkStatus()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("성공 - 출근 기록 없음")
        void getTodayAttendance_Empty() {
            // given
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();
            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.empty());

            // when
            AttendanceResponseDTO result = attendanceService.getTodayAttendance(employeeId);

            // then
            assertThat(result).isNull();
        }
    }
}