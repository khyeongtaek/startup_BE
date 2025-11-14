package org.goodee.startup_BE.common.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.enums.EmployeeStatus;
import org.goodee.startup_BE.employee.enums.Position;
import org.goodee.startup_BE.employee.enums.Role;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonCodeServiceImpl implements CommonCodeService {

  private final CommonCodeRepository commonCodeRepository;

  @Override
  public List<CommonCodeResponseDTO>  getAllDepartments() {
    return commonCodeRepository
            .findByCodeStartsWithAndIsDeletedFalse("DP")
            .stream()
            .map(CommonCodeResponseDTO::toDTO)
            .toList();
  }

  @Override
  public List<CommonCodeResponseDTO> getAllEmployeeStatus() {
    return commonCodeRepository
            .findByCodeStartsWithAndIsDeletedFalse(EmployeeStatus.PREFIX)
            .stream()
            .map(CommonCodeResponseDTO::toDTO)
            .toList();
  }

  @Override
  public List<CommonCodeResponseDTO> getAllPositions() {
    return commonCodeRepository
            .findByCodeStartsWithAndIsDeletedFalse(Position.PREFIX)
            .stream()
            .map(CommonCodeResponseDTO::toDTO)
            .toList();
  }

  @Override
  public List<CommonCodeResponseDTO> getAllRole() {
    return commonCodeRepository
            .findByCodeStartsWithAndIsDeletedFalse(Role.PREFIX)
            .stream()
            .map(CommonCodeResponseDTO::toDTO)
            .toList();
  }


}
