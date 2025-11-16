package org.goodee.startup_BE.schedule.service;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;
import org.goodee.startup_BE.schedule.entity.Schedule;
import org.goodee.startup_BE.schedule.exception.InvalidScheduleArgumentException;
import org.goodee.startup_BE.schedule.exception.ScheduleNotFoundException;
import org.goodee.startup_BE.schedule.repository.ScheduleRepository;
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
class ScheduleServiceImplTest {

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CommonCodeRepository commonCodeRepository;

    private Employee mockEmployee;
    private CommonCode mockCategoryWork;
    private Schedule mockSchedule;

    @BeforeEach
    void setUp() {
        mockEmployee = Employee.createEmployee(
                "tester", "테스트", "test@test.com", "010-0000-0000",
                LocalDate.now(), null, null, null, null, null
        );
        mockEmployee.updateInitPassword("1234", null);

        mockCategoryWork = CommonCode.createCommonCode("SC01", "업무", "업무", null, null, 1L, mockEmployee, false);

        mockSchedule = Schedule.createSchedule(
                mockEmployee, "회의", "오전 회의", mockCategoryWork,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1)
        );
    }

    // ======================= CREATE =======================
    @Nested
    @DisplayName("createSchedule() 일정 등록")
    class CreateSchedule {

        @Test
        @DisplayName("성공 - 일정 등록 성공")
        void createSchedule_Success() {
            // given
            ScheduleRequestDTO dto = ScheduleRequestDTO.builder()
                    .employeeId(1L)
                    .title("회의")
                    .content("오전 회의")
                    .categoryCode("SC01")
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusHours(1))
                    .build();

            given(employeeRepository.findById(1L)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("SC", "SC01"))
                    .willReturn(List.of(mockCategoryWork));
            given(scheduleRepository.save(any(Schedule.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            ScheduleResponseDTO result = scheduleService.createSchedule(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("회의");
            assertThat(result.getCategoryName()).isEqualTo(null);
            verify(scheduleRepository, times(1)).save(any(Schedule.class));
        }

        @Test
        @DisplayName("실패 - 직원 정보 없음")
        void createSchedule_Fail_NoEmployee() {
            // given
            ScheduleRequestDTO dto = ScheduleRequestDTO.builder()
                    .employeeId(1L)
                    .categoryCode("WORK")
                    .build();

            given(employeeRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scheduleService.createSchedule(dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("사원 정보를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패 - 카테고리 코드 없음")
        void createSchedule_Fail_NoCategory() {
            // given
            ScheduleRequestDTO dto = ScheduleRequestDTO.builder()
                    .employeeId(1L)
                    .categoryCode("INVALID")
                    .build();

            given(employeeRepository.findById(1L)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("SC", "INVALID"))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> scheduleService.createSchedule(dto))
                    .isInstanceOf(InvalidScheduleArgumentException.class)
                    .hasMessageContaining("유효하지 않은 일정 카테고리 코드입니다");
        }


        // ======================= READ (단일 조회) =======================
        @Nested
        @DisplayName("getSchedule() 단일 일정 조회")
        class GetSchedule {

            @Test
            @DisplayName("성공 - 일정 존재")
            void getSchedule_Success() {
                // given
                given(scheduleRepository.findById(1L)).willReturn(Optional.of(mockSchedule));

                // when
                ScheduleResponseDTO result = scheduleService.getSchedule(1L);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getTitle()).isEqualTo("회의");
            }

            @Test
            @DisplayName("실패 - 일정 없음")
            void getSchedule_Fail_NotFound() {
                // given
                given(scheduleRepository.findById(1L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> scheduleService.getSchedule(1L))
                        .isInstanceOf(ScheduleNotFoundException.class)
                        .hasMessageContaining("해당 일정을 찾을 수 없습니다");
            }
        }

        // ======================= READ (전체 조회) =======================
        @Nested
        @DisplayName("getAllSchedule() 전체 일정 조회")
        class GetAllSchedule {

            @Test
            @DisplayName("성공 - 데이터 존재")
            void getAllSchedule_Success() {
                // given
                given(scheduleRepository.findByIsDeletedFalse()).willReturn(List.of(mockSchedule));

                // when
                List<ScheduleResponseDTO> result = scheduleService.getAllSchedule();

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTitle()).isEqualTo("회의");
            }

            @Test
            @DisplayName("성공 - 데이터 없음")
            void getAllSchedule_Empty() {
                // given
                given(scheduleRepository.findByIsDeletedFalse()).willReturn(List.of());

                // when
                List<ScheduleResponseDTO> result = scheduleService.getAllSchedule();

                // then
                assertThat(result).isEmpty();
            }
        }

        // ======================= READ (기간별 조회) =======================
        @Nested
        @DisplayName("getAllScheduleByPeriod() 기간별 일정 조회")
        class GetAllScheduleByPeriod {

            @Test
            @DisplayName("성공 - 기간 내 일정 존재")
            void getAllScheduleByPeriod_Success() {
                // given
                LocalDate start = LocalDate.now().minusDays(1);
                LocalDate end = LocalDate.now().plusDays(1);

                given(scheduleRepository.findByStartTimeBetweenAndIsDeletedFalse(any(), any()))
                        .willReturn(List.of(mockSchedule));

                // when
                List<ScheduleResponseDTO> result = scheduleService.getAllScheduleByPeriod(start, end);

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTitle()).isEqualTo("회의");
            }

            @Test
            @DisplayName("성공 - 기간 내 일정 없음")
            void getAllScheduleByPeriod_Empty() {
                // given
                LocalDate start = LocalDate.now().minusDays(1);
                LocalDate end = LocalDate.now().plusDays(1);

                given(scheduleRepository.findByStartTimeBetweenAndIsDeletedFalse(any(), any()))
                        .willReturn(List.of());

                // when
                List<ScheduleResponseDTO> result = scheduleService.getAllScheduleByPeriod(start, end);

                // then
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("실패 - 종료일이 시작일보다 빠름")
            void getAllScheduleByPeriod_Fail_InvalidPeriod() {
                // given
                LocalDate start = LocalDate.now();
                LocalDate end = LocalDate.now().minusDays(1);

                // when & then
                assertThatThrownBy(() -> scheduleService.getAllScheduleByPeriod(start, end))
                        .isInstanceOf(InvalidScheduleArgumentException.class)
                        .hasMessageContaining("종료일은 시작일보다 이후여야 합니다");
            }
        }
    }
}