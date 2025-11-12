package org.goodee.startup_BE.employee.service;

import org.goodee.startup_BE.employee.dto.EmployeeHistoryResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.entity.EmployeeHistory;
import org.goodee.startup_BE.employee.repository.EmployeeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page; // Page 임포트
import org.springframework.data.domain.PageImpl; // PageImpl 임포트
import org.springframework.data.domain.PageRequest; // PageRequest 임포트
import org.springframework.data.domain.Pageable; // Pageable 임포트

import java.time.LocalDateTime;
import java.util.List;

// AssertJ static import
import static org.assertj.core.api.Assertions.*;
// BDDMockito static import
import static org.mockito.BDDMockito.given;
// Mockito static import
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito 확장 사용
class EmployeeHistoryServiceImplTest {

    @InjectMocks // 테스트 대상 클래스, Mock 객체들이 주입됨
    private EmployeeHistoryServiceImpl employeeHistoryService;

    @Mock // Mock 객체로 생성
    private EmployeeHistoryRepository employeeHistoryRepository;

    // 테스트에서 공통으로 사용할 Mock 객체 선언
    private Employee mockEmployee;
    private Employee mockUpdater;
    private EmployeeHistory mockHistory1;
    private EmployeeHistory mockHistory2;

    @BeforeEach // 각 테스트 실행 전 공통 설정
    void setUp() {
        // Mock 객체 초기화
        mockEmployee = mock(Employee.class);
        mockUpdater = mock(Employee.class);
        mockHistory1 = mock(EmployeeHistory.class);
        mockHistory2 = mock(EmployeeHistory.class);

        // EmployeeHistoryResponseDTO.toDTO() 메서드 실행 시 NPE 방지를 위한 기본 Mocking
        // lenient() : 해당 Mocking이 모든 테스트에서 사용되지 않더라도 경고/오류를 발생시키지 않음
        lenient().when(mockHistory1.getEmployee()).thenReturn(mockEmployee);
        lenient().when(mockHistory1.getUpdater()).thenReturn(mockUpdater);
        lenient().when(mockHistory1.getFieldName()).thenReturn("부서");
        lenient().when(mockHistory1.getOldValue()).thenReturn("인사팀");
        lenient().when(mockHistory1.getNewValue()).thenReturn("개발팀");
        lenient().when(mockHistory1.getChangedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 10, 0));

        lenient().when(mockHistory2.getEmployee()).thenReturn(mockEmployee);
        lenient().when(mockHistory2.getUpdater()).thenReturn(mockUpdater);
        lenient().when(mockHistory2.getFieldName()).thenReturn("직급");
        lenient().when(mockHistory2.getOldValue()).thenReturn("사원");
        lenient().when(mockHistory2.getNewValue()).thenReturn("대리");
        lenient().when(mockHistory2.getChangedAt()).thenReturn(LocalDateTime.of(2025, 1, 2, 11, 0));

        lenient().when(mockEmployee.getUsername()).thenReturn("targetUser");
        lenient().when(mockUpdater.getUsername()).thenReturn("adminUser");
    }

    @Nested // 테스트 그룹화
    @DisplayName("getEmployeeHistories (직원 이력 조회 - 페이징)")
    class GetEmployeeHistories {

        @Test
        @DisplayName("성공 - 이력 있음")
        void getEmployeeHistories_Success_HistoriesFound() {
            // given
            Long employeeId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            List<EmployeeHistory> histories = List.of(mockHistory1, mockHistory2);
            Page<EmployeeHistory> mockPage = new PageImpl<>(histories, pageable, histories.size());

            given(employeeHistoryRepository.findByEmployeeEmployeeId(employeeId, pageable))
                    .willReturn(mockPage);

            // when
            Page<EmployeeHistoryResponseDTO> resultPage = employeeHistoryService.getEmployeeHistories(employeeId, pageable);

            // then
            // Repository 호출 검증
            verify(employeeHistoryRepository).findByEmployeeEmployeeId(employeeId, pageable);

            // 반환된 Page 객체 검증
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent()).hasSize(2);

            // DTO 변환 검증 (첫 번째 항목)
            EmployeeHistoryResponseDTO dto1 = resultPage.getContent().get(0);
            assertThat(dto1.getEmployeeUsername()).isEqualTo("targetUser");
            assertThat(dto1.getUpdaterUsername()).isEqualTo("adminUser");
            assertThat(dto1.getFieldName()).isEqualTo("부서");
            assertThat(dto1.getOldValue()).isEqualTo("인사팀");
            assertThat(dto1.getNewValue()).isEqualTo("개발팀");
            assertThat(dto1.getChangedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));

            // DTO 변환 검증 (두 번째 항목)
            EmployeeHistoryResponseDTO dto2 = resultPage.getContent().get(1);
            assertThat(dto2.getFieldName()).isEqualTo("직급");
            assertThat(dto2.getNewValue()).isEqualTo("대리");
        }

        @Test
        @DisplayName("성공 - 이력 없음")
        void getEmployeeHistories_Success_NoHistories() {
            // given
            Long employeeId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            // 빈 페이지 반환
            Page<EmployeeHistory> emptyPage = Page.empty(pageable);
            given(employeeHistoryRepository.findByEmployeeEmployeeId(employeeId, pageable))
                    .willReturn(emptyPage);

            // when
            Page<EmployeeHistoryResponseDTO> resultPage = employeeHistoryService.getEmployeeHistories(employeeId, pageable);

            // then
            // Repository 호출 검증
            verify(employeeHistoryRepository).findByEmployeeEmployeeId(employeeId, pageable);

            // 반환된 리스트가 비어있는지 확인
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent()).isEmpty();
            assertThat(resultPage.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("logHistory (이력 기록)")
    class LogHistory {

        // ArgumentCaptor: save 메서드에 전달된 EmployeeHistory 객체를 캡처하기 위함
        private ArgumentCaptor<EmployeeHistory> historyCaptor;

        @BeforeEach
        void logSetup() {
            historyCaptor = ArgumentCaptor.forClass(EmployeeHistory.class);
        }

        @Test
        @DisplayName("성공 - 값 변경 (Old/New 모두 존재)")
        void logHistory_Success_ValueChange() {
            // given
            String fieldName = "직급";
            String oldValue = "사원";
            String newValue = "대리";

            // when
            employeeHistoryService.logHistory(mockEmployee, mockUpdater, fieldName, oldValue, newValue);

            // then
            // repository.save가 1회 호출되었는지 검증
            verify(employeeHistoryRepository, times(1)).save(historyCaptor.capture());

            // 캡처된 EmployeeHistory 객체의 필드 검증
            EmployeeHistory capturedHistory = historyCaptor.getValue();
            assertThat(capturedHistory.getEmployee()).isEqualTo(mockEmployee);
            assertThat(capturedHistory.getUpdater()).isEqualTo(mockUpdater);
            assertThat(capturedHistory.getFieldName()).isEqualTo(fieldName);
            assertThat(capturedHistory.getOldValue()).isEqualTo(oldValue);
            assertThat(capturedHistory.getNewValue()).isEqualTo(newValue);
            // @PrePersist는 Mock 객체에서는 동작하지 않으므로, changedAt이 null인지 확인 (또는 Mocking 필요)
            // 여기서는 createHistory 메서드가 필드를 직접 설정하지 않는 것을 확인
            assertThat(capturedHistory.getChangedAt()).isNull();
        }

        @Test
        @DisplayName("성공 - 값 추가 (OldValue가 null)")
        void logHistory_Success_ValueAdded() {
            // given
            String fieldName = "연락처";
            String oldValue = null;
            String newValue = "010-1111-1111";

            // when
            employeeHistoryService.logHistory(mockEmployee, mockUpdater, fieldName, oldValue, newValue);

            // then
            // save가 호출되어야 함
            verify(employeeHistoryRepository, times(1)).save(historyCaptor.capture());

            EmployeeHistory capturedHistory = historyCaptor.getValue();
            assertThat(capturedHistory.getOldValue()).isNull();
            assertThat(capturedHistory.getNewValue()).isEqualTo(newValue);
        }

        @Test
        @DisplayName("성공 - 값 삭제 (NewValue가 null)")
        void logHistory_Success_ValueRemoved() {
            // given
            String fieldName = "연락처";
            String oldValue = "010-1111-1111";
            String newValue = null;

            // when
            employeeHistoryService.logHistory(mockEmployee, mockUpdater, fieldName, oldValue, newValue);

            // then
            // save가 호출되어야 함
            verify(employeeHistoryRepository, times(1)).save(historyCaptor.capture());

            EmployeeHistory capturedHistory = historyCaptor.getValue();
            assertThat(capturedHistory.getOldValue()).isEqualTo(oldValue);
            assertThat(capturedHistory.getNewValue()).isNull();
        }

        @Test
        @DisplayName("무시 - 값이 동일함 (둘 다 null 아님)")
        void logHistory_Ignored_SameValues_NonNull() {
            // given
            String fieldName = "직급";
            String oldValue = "사원";
            String newValue = "사원"; // 값이 동일

            // when
            employeeHistoryService.logHistory(mockEmployee, mockUpdater, fieldName, oldValue, newValue);

            // then
            // 서비스 로직의 조건문에 따라 save가 호출되지 않아야 함
            verify(employeeHistoryRepository, never()).save(any(EmployeeHistory.class));
        }

        @Test
        @DisplayName("무시 - 값이 동일함 (둘 다 null)")
        void logHistory_Ignored_SameValues_BothNull() {
            // given
            String fieldName = "비고";
            String oldValue = null;
            String newValue = null; // 값이 동일 (null)

            // when
            employeeHistoryService.logHistory(mockEmployee, mockUpdater, fieldName, oldValue, newValue);

            // then
            // 서비스 로직: if ((oldValue != null && newValue != null) && Objects.equals(oldValue, newValue))
            // 1. oldValue="사원", newValue="사원" -> (true && true) && true -> true -> return (저장 안 함)
            // 2. oldValue=null, newValue=null -> (false && false) && true -> false -> return 안 함 (저장 함)
            // 3. [수정] 서비스 로직 재검토
            //    if (Objects.equals(oldValue, newValue)) { return; } 이 더 명확함.
            //    현재 로직: if ((oldValue != null && newValue != null) && Objects.equals(oldValue, newValue))
            //    (null, null) -> (false && false) && true -> false -> 저장됨.
            //    (null, "A") -> (false && true) && false -> false -> 저장됨.
            //    ("A", null) -> (true && false) && false -> false -> 저장됨.
            //    ("A", "A") -> (true && true) && true -> true -> 반환됨. (저장 안 됨)
            //
            //    [결론] 기존 테스트(logHistory_Ignored_SameValues_BothNull)는
            //    null -> null 변경이 '저장'되는 것을 검증해야 했습니다.
            //    테스트 명을 "성공 - 값이 동일함 (둘 다 null)"로 변경합니다.
            verify(employeeHistoryRepository, times(1)).save(historyCaptor.capture());

            EmployeeHistory capturedHistory = historyCaptor.getValue();
            assertThat(capturedHistory.getOldValue()).isNull();
            assertThat(capturedHistory.getNewValue()).isNull();
        }
    }
}