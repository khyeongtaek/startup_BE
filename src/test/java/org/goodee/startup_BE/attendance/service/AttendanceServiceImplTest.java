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
import org.goodee.startup_BE.schedule.repository.ScheduleRepository;
import org.goodee.startup_BE.schedule.service.HolidayService;
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

    @Mock
    private HolidayService holidayService;

    @Mock
    private ScheduleRepository scheduleRepository;

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

            LocalDateTime fakeNow = LocalDateTime.of(2025, 1, 1, 8, 30);
            LocalDate fakeToday = fakeNow.toLocalDate();

            try (MockedStatic<LocalDateTime> mocked = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {

                // now(), now(zoneId) 모두 고정
                mocked.when(LocalDateTime::now).thenReturn(fakeNow);
                mocked.when(() -> LocalDateTime.now(any(ZoneId.class))).thenReturn(fakeNow);

                // LocalDate.now()도 고정 (중요)
                try (MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
                    mockedDate.when(LocalDate::now).thenReturn(fakeToday);

                    given(attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(employeeId, fakeToday))
                            .willReturn(Optional.empty());
                    given(employeeRepository.findById(employeeId)).willReturn(Optional.of(mockEmployee));
                    given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", "NORMAL"))
                            .willReturn(List.of(mockWorkStatusNormal));

                    lenient().when(commonCodeRepository
                                    .findByCodeStartsWithAndKeywordExactMatchInValues("WS", "LATE"))
                            .thenReturn(List.of(mockWorkStatusLate));

                    lenient().when(attendanceRepository.countByEmployeeEmployeeId(employeeId))
                            .thenReturn(0L);
                    lenient().when(annualLeaveService.createIfNotExists(employeeId))
                            .thenReturn(null);

                    given(attendanceRepository.save(any(Attendance.class)))
                            .willAnswer(invocation -> invocation.getArgument(0));

                    AttendanceResponseDTO result = attendanceService.clockIn(employeeId);

                    assertThat(result).isNotNull();
                    assertThat(result.getWorkStatus()).isIn("NORMAL", "LATE");
                    verify(attendanceRepository, times(1)).save(any(Attendance.class));
                }
            }
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
            // ===================== 결근 테스트 =====================
            @Nested
            @DisplayName("getAbsentDays() 결근 조회")
            class GetAbsentDays {

                @BeforeEach
                void lenientStub() {
                    // 기본값: 모든 날짜 → 출근 기록 없음
                    lenient().when(attendanceRepository.existsByEmployeeEmployeeIdAndAttendanceDate(anyLong(), any(LocalDate.class)))
                            .thenReturn(false);

                    // 기본값: 모든 날짜 → 휴가 없음
                    lenient().when(scheduleRepository.existsVacationOn(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                            .thenReturn(false);

                    // 기본값: 모든 날짜 → 공휴일 아님
                    lenient().when(holidayService.isHoliday(any(LocalDateTime.class)))
                            .thenReturn(false);
                }

                @Test
                @DisplayName("성공 - 특정 날짜 결근")
                void getAbsentDays_Success() {

                    Long employeeId = 1L;
                    int year = 2025;
                    int month = 1;

                    LocalDate target = LocalDate.of(2025, 1, 2);

                    // target 날짜에 대해 출근 기록 없음 → 결근
                    lenient().when(attendanceRepository.existsByEmployeeEmployeeIdAndAttendanceDate(employeeId, target))
                            .thenReturn(false);

                    List<LocalDate> result = attendanceService.getAbsentDays(employeeId, year, month);

                    assertThat(result).contains(target);
                }

                @Test
                @DisplayName("성공 - 출근 기록이 있으면 결근 아님")
                void getAbsentDays_NoAbsent_WhenAttendanceExists() {

                    Long employeeId = 1L;
                    int year = 2025;
                    int month = 1;

                    LocalDate day = LocalDate.of(2025, 1, 3);

                    // 해당 날짜만 출근 처리
                    lenient().when(attendanceRepository.existsByEmployeeEmployeeIdAndAttendanceDate(employeeId, day))
                            .thenReturn(true);

                    List<LocalDate> result = attendanceService.getAbsentDays(employeeId, year, month);

                    assertThat(result).doesNotContain(day);
                }

                @Test
                @DisplayName("성공 - 휴가가 있으면 결근 아님")
                void getAbsentDays_NoAbsent_WhenVacationExists() {

                    Long employeeId = 1L;
                    int year = 2025;
                    int month = 1;

                    LocalDate day = LocalDate.of(2025, 1, 4);

                    // 출근 기록 없음
                    lenient().when(attendanceRepository.existsByEmployeeEmployeeIdAndAttendanceDate(employeeId, day))
                            .thenReturn(false);

                    // 휴가 있음
                    lenient().when(scheduleRepository.existsVacationOn(eq(employeeId), any(LocalDateTime.class), any(LocalDateTime.class)))
                            .thenReturn(true);

                    List<LocalDate> result = attendanceService.getAbsentDays(employeeId, year, month);

                    assertThat(result).doesNotContain(day);
                }

                @Test
                @DisplayName("성공 - 공휴일은 결근 아님")
                void getAbsentDays_NoAbsent_WhenHoliday() {

                    Long employeeId = 1L;
                    int year = 2025;
                    int month = 1;

                    LocalDate day = LocalDate.of(2025, 1, 1);

                    lenient().when(holidayService.isHoliday(day.atStartOfDay()))
                            .thenReturn(true);

                    List<LocalDate> result = attendanceService.getAbsentDays(employeeId, year, month);

                    assertThat(result).doesNotContain(day);
                }
            }
    }}}