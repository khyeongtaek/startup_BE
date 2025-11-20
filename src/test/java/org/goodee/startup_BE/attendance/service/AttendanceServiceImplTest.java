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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

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

    @Mock
    private AnnualLeaveService annualLeaveService;

    @Mock
    private AttendanceWorkHistoryService attendanceWorkHistoryService;

    private Employee mockEmployee;
    private CommonCode mockWorkStatusNormal;
    private CommonCode mockWorkStatusOut;
    private CommonCode mockWorkStatusEarlyLeave;
    private CommonCode mockWorkStatusLate;
    private Attendance mockAttendanceToday;

    @BeforeEach
    void setUp() {
        mockEmployee = mock(Employee.class);
        mockWorkStatusNormal = mock(CommonCode.class);
        mockWorkStatusOut = mock(CommonCode.class);
        mockWorkStatusEarlyLeave = mock(CommonCode.class);
        mockWorkStatusLate = mock(CommonCode.class);
        mockAttendanceToday = mock(Attendance.class);

        lenient().when(mockEmployee.getEmployeeId()).thenReturn(1L);
        lenient().when(mockEmployee.getName()).thenReturn("테스트 사원");
        lenient().when(mockWorkStatusNormal.getValue1()).thenReturn("NORMAL");
        lenient().when(mockWorkStatusOut.getValue1()).thenReturn("CLOCK_OUT");
        lenient().when(mockWorkStatusEarlyLeave.getValue1()).thenReturn("EARLY_LEAVE");
        lenient().when(mockWorkStatusLate.getValue1()).thenReturn("LATE");
    }

    // ===================== 출근 테스트 =====================
    @Nested
    @DisplayName("clockIn() 출근 등록")
    class ClockIn {

        @Test
        @DisplayName("성공 - 정상 출근")
        void clockIn_Success() {
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();

            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.empty());
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "NORMAL"))
                    .willReturn(List.of(mockWorkStatusNormal));
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "LATE"))
                    .willReturn(List.of(mockWorkStatusLate));
            given(attendanceRepository.countByEmployeeEmployeeId(employeeId)).willReturn(0L);
            given(annualLeaveService.createIfNotExists(employeeId)).willReturn(null);
            given(attendanceRepository.save(any(Attendance.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            AttendanceResponseDTO result = attendanceService.clockIn(employeeId);

            assertThat(result).isNotNull();
            assertThat(result.getWorkStatus()).isIn("NORMAL", "LATE");
            verify(attendanceRepository, times(1)).save(any(Attendance.class));
        }

        @Test
        @DisplayName("실패 - 직원 정보 없음")
        void clockIn_Fail_NoEmployee() {
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();

            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.empty());
            given(employeeRepository.findById(employeeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.clockIn(employeeId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("사원 정보를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패 - NORMAL 코드 없음")
        void clockIn_Fail_NoCode() {
            Long employeeId = 1L;
            LocalDate today = LocalDate.now();

            given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                    .willReturn(Optional.empty());
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(mockEmployee));

            // NORMAL 코드 없음
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "NORMAL"))
                    .willReturn(List.of());

            assertThatThrownBy(() -> attendanceService.clockIn(employeeId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("WS");
        }

        // ===================== 퇴근 테스트 =====================
        @Nested
        @DisplayName("clockOut() 퇴근 등록")
        class ClockOut {

            @Test
            @DisplayName("성공 - 정상 퇴근 (18:00 이후 → CLOCK_OUT)")
            void clockOut_Success_Normal() {

                Long employeeId = 1L;

                LocalDateTime fakeNow = LocalDateTime.of(2025, 1, 1, 18, 1);

                try (MockedStatic<LocalDateTime> mocked = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {

                    // now() mocking
                    mocked.when(LocalDateTime::now).thenReturn(fakeNow);

                    // now(ZoneId) mocking (logback / timezone 내부 호출 대응)
                    mocked.when(() -> LocalDateTime.now(any(ZoneId.class)))
                            .thenReturn(fakeNow);

                    Attendance attendance = Attendance.createAttendance(
                            mockEmployee,
                            LocalDate.of(2025, 1, 1),
                            mockWorkStatusNormal
                    );

                    attendance.update(LocalDateTime.of(2025, 1, 1, 9, 0), null);

                    given(attendanceRepository.findCurrentWorkingRecord(employeeId))
                            .willReturn(Optional.of(attendance));

                    given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "CLOCK_OUT"))
                            .willReturn(List.of(mockWorkStatusOut));

                    given(attendanceRepository.save(any()))
                            .willAnswer(invocation -> invocation.getArgument(0));

                    AttendanceResponseDTO result = attendanceService.clockOut(employeeId);

                    assertThat(result.getWorkStatus()).isEqualTo("CLOCK_OUT");
                }
            }

            @Test
            @DisplayName("실패 - 출근 기록 없음")
            void clockOut_Fail_NoRecord() {
                Long employeeId = 1L;

                given(attendanceRepository.findCurrentWorkingRecord(employeeId))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() -> attendanceService.clockOut(employeeId))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("출근 기록이 없습니다");
            }

        // ===================== 오늘 출근 조회 =====================
        @Nested
        @DisplayName("getTodayAttendance() 오늘 근태 조회")
        class GetTodayAttendance {

            @Test
            @DisplayName("성공 - 오늘 출근 기록 존재")
            void getTodayAttendance_Success() {
                Long employeeId = 1L;
                LocalDate today = LocalDate.now();

                Attendance attendance = Attendance.createAttendance(mockEmployee, today, mockWorkStatusNormal);

                given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, today))
                        .willReturn(Optional.of(attendance));

                AttendanceResponseDTO result = attendanceService.getTodayAttendance(employeeId);

                assertThat(result).isNotNull();
                assertThat(result.getWorkStatus()).isEqualTo("NORMAL", "LATE");
            }
        }
    }}}