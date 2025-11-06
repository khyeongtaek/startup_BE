// CommonCodeServiceImplTest.java 파일의 전체 내용

package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO; // DTO 임포트 추가
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.enums.EmployeeStatus; // Enum 임포트 추가
import org.goodee.startup_BE.employee.enums.Position; // Enum 임포트 추가
import org.goodee.startup_BE.employee.enums.Role; // Enum 임포트 추가
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommonCodeServiceImplTest {

  @Mock
  private CommonCodeRepository commonCodeRepository;

  @InjectMocks
  private CommonCodeServiceImpl commonCodeService;

  @Test
  @DisplayName("부서 목록 조회 성공")
  void getAllDepartments_Success() {
    // given
    // 테스트용 데이터 생성
    CommonCode dept1 = CommonCode.createCommonCode(
            "DP1", "개발부", "DEV", null, null, 1L, null
    );
    CommonCode dept2 = CommonCode.createCommonCode(
            "DP2", "인사부", "HR", null, null, 2L, null
    );

    // 레포지토리 메서드 호출 시 반환될 값 설정
    given(commonCodeRepository.findAllDepartments()).willReturn(List.of(dept1, dept2));

    // when
    // 테스트할 서비스 메서드 호출
    List<CommonCode> result = commonCodeService.getAllDepartments();

    // then
    // 결과 검증
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCodeDescription()).isEqualTo("개발부");
    assertThat(result.get(1).getCodeDescription()).isEqualTo("인사부");

    // 메서드가 1번 호출되었는지 검증
    verify(commonCodeRepository, times(1)).findAllDepartments();
  }

  @Test
  @DisplayName("재직 상태 목록 조회 성공")
  void getAllEmployeeStatus_Success() {
    // given
    // 테스트용 데이터 생성 (ES: EmployeeStatus)
    CommonCode status1 = CommonCode.createCommonCode(
            "ES1", "재직", "ACTIVE", null, null, 1L, null
    );
    CommonCode status2 = CommonCode.createCommonCode(
            "ES2", "휴직", "ON_LEAVE", null, null, 2L, null
    );
    List<CommonCode> mockList = List.of(status1, status2);

    // 레포지토리 메서드 호출 시 반환될 값 설정 (PREFIX 사용)
    given(commonCodeRepository.findByCodeStartsWithAndIsDeletedFalse(EmployeeStatus.PREFIX))
            .willReturn(mockList);

    // when
    // 테스트할 서비스 메서드 호출
    List<CommonCodeResponseDTO> result = commonCodeService.getAllEmployeeStatus();

    // then
    // 결과 검증 (DTO로 변환되었는지 확인)
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCodeDescription()).isEqualTo("재직");
    assertThat(result.get(0).getCode()).isEqualTo("ES1");
    assertThat(result.get(1).getCodeDescription()).isEqualTo("휴직");
    assertThat(result.get(1).getCode()).isEqualTo("ES2");

    // 메서드가 올바른 PREFIX로 1번 호출되었는지 검증
    verify(commonCodeRepository, times(1))
            .findByCodeStartsWithAndIsDeletedFalse(EmployeeStatus.PREFIX);
  }

  @Test
  @DisplayName("직급 목록 조회 성공")
  void getAllPositions_Success() {
    // given
    // 테스트용 데이터 생성 (PS: Position)
    CommonCode pos1 = CommonCode.createCommonCode(
            "PS1", "사원", "STAFF", null, null, 1L, null
    );
    CommonCode pos2 = CommonCode.createCommonCode(
            "PS2", "주임", "SENIOR_STAFF", null, null, 2L, null
    );
    List<CommonCode> mockList = List.of(pos1, pos2);

    // 레포지토리 메서드 호출 시 반환될 값 설정
    given(commonCodeRepository.findByCodeStartsWithAndIsDeletedFalse(Position.PREFIX))
            .willReturn(mockList);

    // when
    // 테스트할 서비스 메서드 호출
    List<CommonCodeResponseDTO> result = commonCodeService.getAllPositions();

    // then
    // 결과 검증
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCodeDescription()).isEqualTo("사원");
    assertThat(result.get(0).getCode()).isEqualTo("PS1");
    assertThat(result.get(1).getCodeDescription()).isEqualTo("주임");
    assertThat(result.get(1).getCode()).isEqualTo("PS2");

    // 메서드가 올바른 PREFIX로 1번 호출되었는지 검증
    verify(commonCodeRepository, times(1))
            .findByCodeStartsWithAndIsDeletedFalse(Position.PREFIX);
  }

  @Test
  @DisplayName("권한 목록 조회 성공")
  void getAllRole_Success() {
    // given
    // 테스트용 데이터 생성 (AU: Role - 아마도 Authority에서 따온 듯)
    CommonCode role1 = CommonCode.createCommonCode(
            "AU1", "일반사용자", "ROLE_USER", null, null, 1L, null
    );
    CommonCode role2 = CommonCode.createCommonCode(
            "AU2", "관리자", "ROLE_ADMIN", null, null, 2L, null
    );
    List<CommonCode> mockList = List.of(role1, role2);

    // 레포지토리 메서드 호출 시 반환될 값 설정
    given(commonCodeRepository.findByCodeStartsWithAndIsDeletedFalse(Role.PREFIX))
            .willReturn(mockList);

    // when
    // 테스트할 서비스 메서드 호출
    List<CommonCodeResponseDTO> result = commonCodeService.getAllRole();

    // then
    // 결과 검증
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCodeDescription()).isEqualTo("일반사용자");
    assertThat(result.get(0).getCode()).isEqualTo("AU1");
    assertThat(result.get(1).getCodeDescription()).isEqualTo("관리자");
    assertThat(result.get(1).getCode()).isEqualTo("AU2");

    // 메서드가 올바른 PREFIX로 1번 호출되었는지 검증
    verify(commonCodeRepository, times(1))
            .findByCodeStartsWithAndIsDeletedFalse(Role.PREFIX);
  }
}