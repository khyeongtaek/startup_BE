package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
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
    CommonCode dept1 = CommonCode.createCommonCode(
            "DP1", "개발부", "DEV", null, null, 1L, null
    );
    CommonCode dept2 = CommonCode.createCommonCode(
            "DP2", "인사부", "HR", null, null, 2L, null
    );

    given(commonCodeRepository.findAllDepartments()).willReturn(List.of(dept1, dept2));

    // when
    List<CommonCode> result = commonCodeService.getAllDepartments();

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCodeDescription()).isEqualTo("개발부");
    assertThat(result.get(1).getCodeDescription()).isEqualTo("인사부");

    verify(commonCodeRepository, times(1)).findAllDepartments();
  }
}
