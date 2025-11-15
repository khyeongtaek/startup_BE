package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;

import java.util.List;

public interface CommonCodeService {

    // 전체 부서 조회
    List<CommonCode> getAllDepartments();

    // 전체 재직상태 조회
    List<CommonCodeResponseDTO> getAllEmployeeStatus();

    // 전체 직급 조회
    List<CommonCodeResponseDTO> getAllPositions();

    // 전체 권한 조회
    List<CommonCodeResponseDTO> getAllRole();

    // 휴가 목록 조회
    List<CommonCodeResponseDTO> getVacationTypes();
}
