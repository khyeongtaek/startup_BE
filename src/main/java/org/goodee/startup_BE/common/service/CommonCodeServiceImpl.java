package org.goodee.startup_BE.common.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonCodeServiceImpl implements CommonCodeService {

  private final CommonCodeRepository commonCodeRepository;

  @Override
  public List<CommonCode>  getAllDepartments() {
    return commonCodeRepository.findAllDepartments();
  }


}
