package org.goodee.startup_BE.approval.service;

import org.goodee.startup_BE.approval.dto.ApprovalTemplateResponseDTO;
import org.goodee.startup_BE.approval.entity.ApprovalTemplate;
import org.goodee.startup_BE.approval.repository.ApprovalTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalTemplateSerivceImplTest {

    @InjectMocks
    private ApprovalTemplateSerivceImpl approvalTemplateService;

    @Mock
    private ApprovalTemplateRepository approvalTemplateRepository;

    // 테스트용 공통 객체
    private ApprovalTemplate mockTemplate1;
    private ApprovalTemplate mockTemplate2;
    private Long templateId1 = 1L;
    private Long templateId2 = 2L;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        // Mock 객체 초기화
        mockTemplate1 = mock(ApprovalTemplate.class);
        mockTemplate2 = mock(ApprovalTemplate.class);
        testTime = LocalDateTime.now();

        // DTO 변환(toDTO) 시 사용되는 핵심 Mocking 설정
        // lenient()를 적용하여 테스트 간 충돌 방지
        lenient().when(mockTemplate1.getTemplateId()).thenReturn(templateId1);
        lenient().when(mockTemplate1.getTemplateName()).thenReturn("휴가 신청서");
        lenient().when(mockTemplate1.getContent()).thenReturn("휴가 내용");
        lenient().when(mockTemplate1.getCreatedAt()).thenReturn(testTime);

        lenient().when(mockTemplate2.getTemplateId()).thenReturn(templateId2);
        lenient().when(mockTemplate2.getTemplateName()).thenReturn("출장 보고서");
    }

    @Nested
    @DisplayName("getAllTemplates (삭제되지 않은 모든 양식 조회)")
    class GetAllTemplates {

        @Test
        @DisplayName("성공 - 양식이 2개 있을 때")
        void getAllTemplates_Success() {
            // given: Repository가 2개의 템플릿 리스트를 반환하도록 Mocking
            List<ApprovalTemplate> templateList = List.of(mockTemplate1, mockTemplate2);
            when(approvalTemplateRepository.findAllByIsDeletedFalse()).thenReturn(templateList);

            // when: 서비스 메서드 호출
            List<ApprovalTemplateResponseDTO> resultList = approvalTemplateService.getAllTemplates();

            // then: 결과 검증
            assertThat(resultList).isNotNull();
            assertThat(resultList).hasSize(2);

            // DTO 변환(toDTO) 검증
            assertThat(resultList.get(0).getTemplateId()).isEqualTo(templateId1);
            assertThat(resultList.get(0).getTemplateName()).isEqualTo("휴가 신청서");
            assertThat(resultList.get(1).getTemplateId()).isEqualTo(templateId2);
            assertThat(resultList.get(1).getTemplateName()).isEqualTo("출장 보고서");

            // Repository 호출 검증
            verify(approvalTemplateRepository, times(1)).findAllByIsDeletedFalse();
        }

        @Test
        @DisplayName("성공 - 양식이 하나도 없을 때 (빈 리스트)")
        void getAllTemplates_Success_Empty() {
            // given: Repository가 빈 리스트를 반환하도록 Mocking
            when(approvalTemplateRepository.findAllByIsDeletedFalse()).thenReturn(Collections.emptyList());

            // when: 서비스 메서드 호출
            List<ApprovalTemplateResponseDTO> resultList = approvalTemplateService.getAllTemplates();

            // then: 결과 검증
            assertThat(resultList).isNotNull();
            assertThat(resultList).isEmpty();

            // Repository 호출 검증
            verify(approvalTemplateRepository, times(1)).findAllByIsDeletedFalse();
        }
    }

    @Nested
    @DisplayName("getTemplate (특정 양식 조회)")
    class GetTemplate {

        @Test
        @DisplayName("성공")
        void getTemplate_Success() {
            // given: Repository가 ID로 조회 시 Optional<Template>을 반환하도록 Mocking
            when(approvalTemplateRepository.findByTemplateIdAndIsDeletedFalse(templateId1)).thenReturn(Optional.of(mockTemplate1));

            // when: 서비스 메서드 호출
            ApprovalTemplateResponseDTO resultDTO = approvalTemplateService.getTemplate(templateId1);

            // then: 결과 검증 (DTO 변환 검증)
            assertThat(resultDTO).isNotNull();
            assertThat(resultDTO.getTemplateId()).isEqualTo(templateId1);
            assertThat(resultDTO.getTemplateName()).isEqualTo("휴가 신청서");
            assertThat(resultDTO.getContent()).isEqualTo("휴가 내용");
            assertThat(resultDTO.getCreatedAt()).isEqualTo(testTime);

            // Repository 호출 검증
            verify(approvalTemplateRepository, times(1)).findByTemplateIdAndIsDeletedFalse(templateId1);
        }

        @Test
        @DisplayName("실패 - ID에 해당하는 양식이 없음 (IllegalArgumentException)")
        void getTemplate_Fail_NotFound() {
            // given: Repository가 빈 Optional을 반환하도록 Mocking
            Long nonExistentId = 999L;
            when(approvalTemplateRepository.findByTemplateIdAndIsDeletedFalse(nonExistentId)).thenReturn(Optional.empty());

            // when & then: 예외 발생 검증
            assertThatThrownBy(() -> approvalTemplateService.getTemplate(nonExistentId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 템플릿을 찾을 수 없습니다");

            // Repository 호출 검증
            verify(approvalTemplateRepository, times(1)).findByTemplateIdAndIsDeletedFalse(nonExistentId);
        }

        // 참고: ApprovalTemplateSerivceImpl 에는 삭제된 ID에 대한 별도 예외가 없습니다.
        // findByIdAndIsDeletedFalse 가 Optional.empty()를 반환하므로 위 NotFound 테스트와 동일하게 동작합니다.
    }
}