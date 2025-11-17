package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.CommonCodeRequestDTO;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;

import java.util.List;

public interface CommonCodeService {

    // 전체 Prefix 조회
    List<CommonCodeResponseDTO> getAllCodePrefixes();

    // Prefix를 바탕으로 대분류 설명인 0번째 값까지 포함한 조회
    List<CommonCodeResponseDTO> getCommonCodeByPrefix(String codePrefix);

    // Prefix를 바탕으로 대분류 설명인 0번째 값을 제외한 조회
    List<CommonCodeResponseDTO> getCommonCodeByPrefixWithoutRoot(String codePrefix);

    // 코드 생성
   CommonCodeResponseDTO createCode(String username, CommonCodeRequestDTO commonCodeRequestDTO);

   // 코드 수정
    CommonCodeResponseDTO updateCode(String username, CommonCodeRequestDTO commonCodeRequestDTO);

    // 휴가 종류
    public List<CommonCodeResponseDTO> getVacationTypes();
}
