// CommonCodeServiceImplTest.java 파일의 전체 내용

package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.CommonCodeRequestDTO;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// AssertJ static import
import static org.assertj.core.api.Assertions.*;

// ArgumentMatchers static import
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

// BDDMockito static import
import static org.mockito.BDDMockito.given;

// Mockito static import
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito 확장 사용
class CommonCodeServiceImplTest {

  @InjectMocks // 테스트 대상 클래스, Mock 객체들이 주입됨
  private CommonCodeServiceImpl commonCodeService;

  @Mock // Mock 객체로 생성
  private CommonCodeRepository commonCodeRepository;

  @Mock // Mock 객체로 생성
  private EmployeeRepository employeeRepository;

  // 테스트에서 공통으로 사용할 Mock 객체 선언
  private Employee mockAdmin;
  private CommonCode mockCode;

  @BeforeEach // 각 테스트 실행 전 공통 설정
  void setUp() {
    // Mock 객체 초기화
    mockAdmin = mock(Employee.class);
    mockCode = mock(CommonCode.class);

    // CommonCodeResponseDTO.toDTO() 메서드 실행 시 NPE 방지를 위한 기본 Mocking
    // lenient() : 해당 Mocking이 모든 테스트에서 사용되지 않더라도 경고/오류를 발생시키지 않음
    lenient().when(mockAdmin.getUsername()).thenReturn("admin");

    lenient().when(mockCode.getCommonCodeId()).thenReturn(1L);
    lenient().when(mockCode.getCode()).thenReturn("DP1");
    lenient().when(mockCode.getCodeDescription()).thenReturn("Test Department");
    lenient().when(mockCode.getCreator()).thenReturn(mockAdmin);
    lenient().when(mockCode.getUpdater()).thenReturn(mockAdmin);
    lenient().when(mockCode.getCreatedAt()).thenReturn(LocalDateTime.now());
    lenient().when(mockCode.getUpdatedAt()).thenReturn(LocalDateTime.now());
    lenient().when(mockCode.getIsDisabled()).thenReturn(false);
  }

  @Nested
  @DisplayName("getAllCodePrefixes (코드 Prefix 전체 조회)")
  class GetAllCodePrefixes {

    @Test
    @DisplayName("성공")
    void getAllCodePrefixes_Success() {
      // given
      CommonCodeResponseDTO dto = CommonCodeResponseDTO.builder().code("DP").build();
      List<CommonCodeResponseDTO> mockDtoList = List.of(dto);

      // Repository가 DTO 리스트를 반환하도록 Mocking
      given(commonCodeRepository.findDistinctCodePrefixes()).willReturn(mockDtoList);

      // when
      List<CommonCodeResponseDTO> result = commonCodeService.getAllCodePrefixes();

      // then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCode()).isEqualTo("DP");
      verify(commonCodeRepository).findDistinctCodePrefixes();
    }

    @Test
    @DisplayName("성공 - 결과 없음")
    void getAllCodePrefixes_Success_Empty() {
      // given
      given(commonCodeRepository.findDistinctCodePrefixes()).willReturn(List.of());

      // when
      List<CommonCodeResponseDTO> result = commonCodeService.getAllCodePrefixes();

      // then
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getCommonCodeByPrefix (Prefix로 코드 조회)")
  class GetCommonCodeByPrefix {

    @Test
    @DisplayName("성공")
    void getCommonCodeByPrefix_Success() {
      // given
      String prefix = "DP";
      given(commonCodeRepository.findByCodeStartsWith(prefix)).willReturn(List.of(mockCode));

      // when
      List<CommonCodeResponseDTO> result = commonCodeService.getCommonCodeByPrefix(prefix);

      // then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCode()).isEqualTo("DP1");
      assertThat(result.get(0).getCreator()).isEqualTo("admin");
      verify(commonCodeRepository).findByCodeStartsWith(prefix);
    }

    @Test
    @DisplayName("성공 - 결과 없음")
    void getCommonCodeByPrefix_Success_Empty() {
      // given
      String prefix = "DP";
      given(commonCodeRepository.findByCodeStartsWith(prefix)).willReturn(List.of());

      // when
      List<CommonCodeResponseDTO> result = commonCodeService.getCommonCodeByPrefix(prefix);

      // then
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getCommonCodeByPrefixWithoutRoot (Root 제외 Prefix로 코드 조회)")
  class GetCommonCodeByPrefixWithoutRoot {

    @Test
    @DisplayName("성공 - Root('DP0') 코드가 필터링되어야 함")
    void getCommonCodeByPrefixWithoutRoot_Success_Filtered() {
      // given
      String prefix = "DP";
      String excludedCode = "DP0";

      CommonCode rootCode = mock(CommonCode.class);
      when(rootCode.getCode()).thenReturn(excludedCode); // "DP0"

      // mockCode는 "DP1" (setUp에서 설정됨)

      // Repository는 "DP0"과 "DP1"을 모두 반환
      given(commonCodeRepository.findByCodeStartsWithAndIsDisabledFalse(prefix))
              .willReturn(List.of(rootCode, mockCode));

      // when
      List<CommonCodeResponseDTO> result = commonCodeService.getCommonCodeByPrefixWithoutRoot(prefix);

      // then
      // "DP0"이 필터링되고 "DP1"만 남아야 함
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCode()).isEqualTo("DP1");
      verify(commonCodeRepository).findByCodeStartsWithAndIsDisabledFalse(prefix);
    }

    @Test
    @DisplayName("성공 - 결과 없음")
    void getCommonCodeByPrefixWithoutRoot_Success_Empty() {
      // given
      String prefix = "DP";
      given(commonCodeRepository.findByCodeStartsWithAndIsDisabledFalse(prefix))
              .willReturn(List.of());

      // when
      List<CommonCodeResponseDTO> result = commonCodeService.getCommonCodeByPrefixWithoutRoot(prefix);

      // then
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("createCode (코드 생성)")
  class CreateCode {

    private CommonCodeRequestDTO request;
    private String adminUsername = "admin";

    @BeforeEach
    void createSetup() {
      request = new CommonCodeRequestDTO();
      request.setCode("DP2");
      request.setCodeDescription("New Department");
      request.setValue1("Value1");
      request.setSortOrder(2L);
      request.setIsDisabled(false);
    }

    @Test
    @DisplayName("성공")
    void createCode_Success() {
      // given
      // 1. 관리자 조회
      given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.of(mockAdmin));

      // 2. commonCodeRepository.save()가 호출될 때
      //    - 어떤 CommonCode 객체가 전달되는지 캡처 (ArgumentCaptor)
      //    - 저장된 결과로 mockCode를 반환 (DTO 변환을 위해)
      ArgumentCaptor<CommonCode> codeCaptor = ArgumentCaptor.forClass(CommonCode.class);
      given(commonCodeRepository.save(codeCaptor.capture())).willReturn(mockCode);

      // when
      CommonCodeResponseDTO result = commonCodeService.createCode(adminUsername, request);

      // then
      // 1. DTO 반환 값 검증 (mockCode 기반)
      assertThat(result).isNotNull();
      assertThat(result.getCode()).isEqualTo("DP1"); // mockCode의 코드
      assertThat(result.getCreator()).isEqualTo("admin");

      // 2. Repository 호출 검증
      verify(employeeRepository).findByUsername(adminUsername);
      verify(commonCodeRepository).save(any(CommonCode.class));

      // 3. save()에 전달된 엔티티(캡처된 객체)의 값 검증
      //    (DTO -> Entity 변환이 올바르게 일어났는지 검증)
      CommonCode capturedCode = codeCaptor.getValue();
      assertThat(capturedCode.getCode()).isEqualTo(request.getCode());
      assertThat(capturedCode.getCodeDescription()).isEqualTo(request.getCodeDescription());
      assertThat(capturedCode.getCreator()).isEqualTo(mockAdmin); // 생성자가 올바르게 설정되었는지
      assertThat(capturedCode.getUpdater()).isEqualTo(mockAdmin); // 수정자가 올바르게 설정되었는지
      assertThat(capturedCode.getIsDisabled()).isFalse();
    }

    @Test
    @DisplayName("실패 - 생성자(관리자) 없음")
    void createCode_Fail_AdminNotFound() {
      // given
      given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commonCodeService.createCode(adminUsername, request))
              .isInstanceOf(ResourceNotFoundException.class)
              .hasMessage("생성자 정보를 찾을 수 없습니다.");

      // save는 절대 호출되면 안 됨
      verify(commonCodeRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("updateCode (코드 수정)")
  class UpdateCode {

    private CommonCodeRequestDTO request;
    private String adminUsername = "admin";
    private Long targetCodeId = 1L;

    @BeforeEach
    void updateSetup() {
      request = new CommonCodeRequestDTO();
      request.setCommonCodeId(targetCodeId);
      request.setCode("DP1-Updated");
      request.setCodeDescription("Updated Description");
      request.setValue1("Updated Value1");
      request.setValue2("Updated Value2");
      request.setValue3("Updated Value3");
      request.setSortOrder(10L);
      request.setIsDisabled(true);
    }

    @Test
    @DisplayName("성공")
    void updateCode_Success() {
      // given
      // 1. 관리자 조회
      given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.of(mockAdmin));
      // 2. 수정할 코드 조회
      given(commonCodeRepository.findById(targetCodeId)).willReturn(Optional.of(mockCode));

      // when
      CommonCodeResponseDTO result = commonCodeService.updateCode(adminUsername, request);

      // then
      // 1. DTO 반환 값 검증 (mockCode 기반)
      assertThat(result).isNotNull();
      assertThat(result.getCommonCodeId()).isEqualTo(targetCodeId);
      assertThat(result.getCreator()).isEqualTo("admin"); // toDTO가 호출되었는지 확인

      // 2. Repository 호출 검증
      verify(employeeRepository).findByUsername(adminUsername);
      verify(commonCodeRepository).findById(targetCodeId);

      // 3. mockCode의 update() 메서드가 정확한 인자들로 호출되었는지 검증
      // CommonCode.java의 update() 시그니처:
      // update(String code, String codeDescription, String value1, String value2,
      //        String value3, Long sortOrder, Employee updater, Boolean isDisabled)
      verify(mockCode).update(
              request.getCode(),
              request.getCodeDescription(),
              request.getValue1(),
              request.getValue2(),
              request.getValue3(),
              request.getSortOrder(),
              mockAdmin, // Employee updater
              request.getIsDisabled()
      );
    }

    @Test
    @DisplayName("실패 - 수정자(관리자) 없음")
    void updateCode_Fail_AdminNotFound() {
      // given
      given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commonCodeService.updateCode(adminUsername, request))
              .isInstanceOf(ResourceNotFoundException.class)
              .hasMessage("생성자 정보를 찾을 수 없습니다.");

      // 관리자 조회 실패 시, 코드 조회나 update는 절대 호출되면 안 됨
      verify(commonCodeRepository, never()).findById(any());
      verify(mockCode, never()).update(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("실패 - 수정할 코드 없음")
    void updateCode_Fail_CodeNotFound() {
      // given
      // 1. 관리자 조회 성공
      given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.of(mockAdmin));
      // 2. 수정할 코드 조회 실패
      given(commonCodeRepository.findById(targetCodeId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commonCodeService.updateCode(adminUsername, request))
              .isInstanceOf(ResourceNotFoundException.class)
              .hasMessage("코드 정보를 찾을 수 없습니다.");

      // update는 절대 호출되면 안 됨
      verify(mockCode, never()).update(any(), any(), any(), any(), any(), any(), any(), any());
    }
  }
}