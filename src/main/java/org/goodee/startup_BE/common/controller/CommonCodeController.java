package org.goodee.startup_BE.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.service.CommonCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Common Code API", description = "공통 코드 조회 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/commoncodes")
public class CommonCodeController {

  private final CommonCodeService commonCodeService;

  @Operation(summary = "전체 부서 목록 조회", description = "시스템에 등록된 모든 부서 목록을 조회합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "부서 목록 조회 성공")
  })
  @GetMapping("/department")
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getDepartments() {

    List<CommonCodeResponseDTO> list = commonCodeService.getAllDepartments();

    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("부서 목록 조회 성공")
            .data(list)
            .build());
  }

  @Operation(summary = "전체 재직상태 목록 조회", description = "시스템에 등록된 모든 재직상태(예: 재직, 휴직) 목록을 조회합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "재직상태 목록 조회 성공")
  })
  @GetMapping("/status")
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getEmployeeStatus() {

    List<CommonCodeResponseDTO> list = commonCodeService.getAllEmployeeStatus();

    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("재직상태 목록 조회 성공")
            .data(list)
            .build());
  }

  @Operation(summary = "전체 직급 목록 조회", description = "시스템에 등록된 모든 직급(예: 사원, 대리) 목록을 조회합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "직급 목록 조회 성공")
  })
  @GetMapping("/position")
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getPositions() {

    List<CommonCodeResponseDTO> list = commonCodeService.getAllPositions();

    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("직급 목록 조회 성공")
            .data(list)
            .build());
  }

  @Operation(summary = "전체 권한 목록 조회", description = "시스템에 등록된 모든 권한(예: ROLE_USER, ROLE_ADMIN) 목록을 조회합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "권한 목록 조회 성공")
  })
  @GetMapping("/role")
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getRoles() {

    List<CommonCodeResponseDTO> list = commonCodeService.getAllRole();

    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("권한 목록 조회 성공")
            .data(list)
            .build());
  }
}