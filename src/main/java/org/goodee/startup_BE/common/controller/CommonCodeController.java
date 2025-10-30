package org.goodee.startup_BE.common.controller;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.service.CommonCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/commoncode")
public class CommonCodeController {

  private final CommonCodeService commonCodeService;

  @GetMapping("/department")
  public ResponseEntity<List<CommonCode>> department() {

    List<CommonCode> list = commonCodeService.getAllDepartments();

    return ResponseEntity.ok(list); // DTO 감싸지 않고 바로 리스트 반환
  }
}
