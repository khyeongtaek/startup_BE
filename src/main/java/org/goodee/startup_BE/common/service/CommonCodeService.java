package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.entity.CommonCode;

import java.util.List;

public interface CommonCodeService {

  // 전체 부서 조회
  List<CommonCode> getAllDepartments();
}
