package org.goodee.startup_BE.common.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

// (JPA Repository)를 사용하기 위해 OrganizationRepository 주입
private final OrganizationRepository organizationRepository;

    @Override
    public List<CommonCodeResponseDTO> getOrganizationTree() {
        // 최상위 부서 조회
        List<CommonCode> rootDepartments = organizationRepository.findRootDepartments();

        // DTO로 변환
        return rootDepartments.stream()
                .map(this::treeView)
                .collect(Collectors.toList());
    }

    // 코드로 부서를 조회하고, 없으면 예외를 던지기 위한 메소드
    @Override
    // 서비스 기능에서 부서하나를 가져오기 위한 코드
    // 값이 있으면 꺼내고, 값이 없으면 예외를 발생시키기 위함.
    // 파라미터 code는 조회할 부서의 고유 코드 값
    public CommonCodeResponseDTO getDepartmentByCode(String code) {

     // JPA Repository 메소드 호출로, 삭제되지 않은 부서(isDeleted = false) 중에서,
     // 코드 값이 일치하는 데이터를 DB에서 찾기위한 코드.
     CommonCode entity = organizationRepository.findByCodeAndIsDeletedFalse(code)

             // IllegalArgumentException - 잘못된 인자가 전달됐을때 발생시키는 예외
             .orElseThrow(() -> new IllegalArgumentException("해당 부서가 존재하지 않습니다"));

        return CommonCodeResponseDTO.toDTO(entity);
    }

     private CommonCodeResponseDTO treeView(CommonCode entity) {

      // 현재 부서 -> DTO 변환
      CommonCodeResponseDTO dto = CommonCodeResponseDTO.toDTO(entity);

      // 현재 부서 기준으로 하위 부서 조회
      List<CommonCode> subDepartments = organizationRepository.findSubDepartments(entity.getCode());

      // 하위 부서가 존재한다면 재귀호출하여 children에 세팅
      // 재귀 호출 : 메소드 안에서 자기 자신을 다시 부르는 것.
      // - 여기서는 treeview()안에서 또 treeview를 호출.
      if (!subDepartments.isEmpty()) {
        List<CommonCodeResponseDTO> commonCodeResponseDTO = subDepartments.stream()
                .map(this::treeView)
                .collect(Collectors.toList());
      }
        return dto;
     }

}
