package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;

import java.util.List;

public interface OrganizationService {

    // 전체 조직도 트리 조회
    List<CommonCodeResponseDTO> getOrganizationTree();

    // 특정 부서 조회
    CommonCodeResponseDTO getDepartmentByCode(String code);



}
