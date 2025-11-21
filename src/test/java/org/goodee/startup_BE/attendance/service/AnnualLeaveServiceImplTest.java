package org.goodee.startup_BE.attendance.service;

import org.goodee.startup_BE.attendance.entity.AnnualLeave;
import org.goodee.startup_BE.attendance.repository.AnnualLeaveRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnualLeaveServiceImplTest {

    @InjectMocks
    private AnnualLeaveServiceImpl annualLeaveService;

    @Mock
    private AnnualLeaveRepository annualLeaveRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private Employee mockEmployee;
    private AnnualLeave mockLeave;

    @BeforeEach
    void setUp() {
        mockEmployee = mock(Employee.class);
        lenient().when(mockEmployee.getEmployeeId()).thenReturn(1L);
        lenient().when(mockEmployee.getName()).thenReturn("홍길동");

        mockLeave = AnnualLeave.builder()
                .leaveId(1L)
                .employee(mockEmployee)
                .totalDays(15.0)
                .usedDays(5.0)
                .year((long) LocalDate.now().getYear())
                .isDeleted(false)
                .build();
    }

    // ===================== 연차 생성 테스트 =====================
    @Nested
    @DisplayName("createIfNotExists() 연차 자동 생성")
    class CreateIfNotExists {

        @Test
        @DisplayName("성공 - 기존 연차 존재 시 기존 데이터 반환")
        void createIfNotExists_AlreadyExists() {
            // given
            Long employeeId = 1L;
            given(annualLeaveRepository.findByEmployeeEmployeeIdAndYear(eq(employeeId), anyLong()))
                    .willReturn(Optional.of(mockLeave));

            // when
            AnnualLeave result = annualLeaveService.createIfNotExists(employeeId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmployee().getName()).isEqualTo("홍길동");
            verify(employeeRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("성공 - 기존 데이터 없음 → 신규 생성")
        void createIfNotExists_CreateNew() {
            // given
            Long employeeId = 1L;
            given(annualLeaveRepository.findByEmployeeEmployeeIdAndYear(eq(employeeId), anyLong()))
                    .willReturn(Optional.empty());
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(mockEmployee));
            given(annualLeaveRepository.save(any(AnnualLeave.class))).willReturn(mockLeave);

            // when
            AnnualLeave result = annualLeaveService.createIfNotExists(employeeId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalDays()).isEqualTo(15L);
            verify(annualLeaveRepository, times(1)).save(any(AnnualLeave.class));
        }

        @Test
        @DisplayName("실패 - 직원 정보 없음")
        void createIfNotExists_Fail_NoEmployee() {
            // given
            Long employeeId = 1L;
            given(annualLeaveRepository.findByEmployeeEmployeeIdAndYear(eq(employeeId), anyLong()))
                    .willReturn(Optional.empty());
            given(employeeRepository.findById(employeeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> annualLeaveService.createIfNotExists(employeeId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("직원 정보를 찾을 수 없습니다");
        }
    }

    // ===================== 연차 조회 테스트 =====================
    @Nested
    @DisplayName("getAnnualLeave() 연차 조회")
    class GetAnnualLeave {

        @Test
        @DisplayName("성공 - 존재하는 연차 반환")
        void getAnnualLeave_Success() {
            // given
            Long employeeId = 1L;
            given(annualLeaveRepository.findByEmployeeEmployeeIdAndYear(eq(employeeId), anyLong()))
                    .willReturn(Optional.of(mockLeave));

            // when
            AnnualLeave result = annualLeaveService.getAnnualLeave(employeeId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUsedDays()).isEqualTo(5L);
            verify(annualLeaveRepository, times(1))
                    .findByEmployeeEmployeeIdAndYear(employeeId, (long) LocalDate.now().getYear());
        }
    }

    // ===================== 연차 사용 테스트 =====================
    @Nested
    @DisplayName("useAnnualLeave() 연차 사용")
    class UseAnnualLeave {

        @Test
        @DisplayName("성공 - 정상적으로 연차 차감")
        void useAnnualLeave_Success() {
            // given
            Long employeeId = 1L;
            given(annualLeaveRepository.findByEmployeeEmployeeIdAndYear(eq(employeeId), anyLong()))
                    .willReturn(Optional.of(mockLeave));
            given(annualLeaveRepository.save(any(AnnualLeave.class))).willReturn(mockLeave);

            // when
            AnnualLeave result = annualLeaveService.useAnnualLeave(employeeId, 2.0);

            // then
            assertThat(result).isNotNull();
            verify(annualLeaveRepository, times(1)).save(any(AnnualLeave.class));
        }

        @Test
        @DisplayName("실패 - 남은 연차 부족")
        void useAnnualLeave_Fail_NotEnough() {
            // given
            Long employeeId = 1L;
            AnnualLeave leave = AnnualLeave.builder()
                    .employee(mockEmployee)
                    .totalDays(2.0)
                    .usedDays(2.0)
                    .year((long) LocalDate.now().getYear())
                    .build();

            given(annualLeaveRepository.findByEmployeeEmployeeIdAndYear(eq(employeeId), anyLong()))
                    .willReturn(Optional.of(leave));

            // when & then
            assertThatThrownBy(() -> annualLeaveService.useAnnualLeave(employeeId, 3.0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("남은 연차가 부족");
        }
    }

    // ===================== 연차 환원 테스트 =====================
    @Nested
    @DisplayName("refundAnnualLeave() 연차 환원")
    class RefundAnnualLeave {

        @Test
        @DisplayName("성공 - 정상적으로 연차 환원")
        void refundAnnualLeave_Success() {
            // given
            Long employeeId = 1L;
            given(annualLeaveRepository.findByEmployeeEmployeeIdAndYear(eq(employeeId), anyLong()))
                    .willReturn(Optional.of(mockLeave));
            given(annualLeaveRepository.save(any(AnnualLeave.class))).willReturn(mockLeave);

            // when
            AnnualLeave result = annualLeaveService.refundAnnualLeave(employeeId, 1.0);

            // then
            assertThat(result).isNotNull();
            verify(annualLeaveRepository, times(1)).save(any(AnnualLeave.class));
        }

        @Test
        @DisplayName("실패 - 사용 일수보다 많이 환원 요청")
        void refundAnnualLeave_Fail_TooMuchRefund() {
            // given
            Long employeeId = 1L;
            AnnualLeave leave = AnnualLeave.builder()
                    .employee(mockEmployee)
                    .totalDays(15.0)
                    .usedDays(1.0)
                    .year((long) LocalDate.now().getYear())
                    .build();

            given(annualLeaveRepository.findByEmployeeEmployeeIdAndYear(eq(employeeId), anyLong()))
                    .willReturn(Optional.of(leave));

            // when & then
            assertThatThrownBy(() -> annualLeaveService.refundAnnualLeave(employeeId, 5.0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("환원할 일수가 사용 일수보다 많습니다.");
        }
    }

    // ===================== 전체 연차 조회 =====================
    @Nested
    @DisplayName("getAllAnnualLeaves() 전체 조회")
    class GetAll {

        @Test
        @DisplayName("성공 - 전체 연차 목록 조회")
        void getAll_Success() {
            // given
            given(annualLeaveRepository.findAll()).willReturn(List.of(mockLeave));

            // when
            List<AnnualLeave> result = annualLeaveService.getAllAnnualLeaves();

            // then
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getEmployee().getName()).isEqualTo("홍길동");
            verify(annualLeaveRepository, times(1)).findAll();
        }
    }
}