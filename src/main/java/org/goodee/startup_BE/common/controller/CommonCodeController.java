package org.goodee.startup_BE.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.dto.CommonCodeRequestDTO;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.service.CommonCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

  @GetMapping
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getAllPrefix() {

    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("전체 대분류 코드 조회 성공")
            .data(commonCodeService.getAllCodePrefixes())
            .build());
  }


  @GetMapping("/prefix/{prefix}")
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getAllCodeOnPrefix(@PathVariable String prefix) {
    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("전체 소분류 코드 조회 성공")
            .data(commonCodeService.getCommonCodeByPrefix(prefix))
            .build());
  }


  @Operation(summary = "공통 코드 생성", description = "새로운 공통 코드를 시스템에 등록합니다. (관리자용)")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "공통 코드 생성 성공")
  })
  @PostMapping
  public ResponseEntity<APIResponseDTO<CommonCodeResponseDTO>> createCode(
          Authentication authentication, @RequestBody CommonCodeRequestDTO request) {
    return ResponseEntity.ok(APIResponseDTO.<CommonCodeResponseDTO>builder()
            .message("공용 코드 저장 성공")
            .data(commonCodeService.createCode(authentication.getName(), request))
            .build());
  }

  @Operation(summary = "공통 코드 수정", description = "기존 공통 코드의 정보를 수정합니다. (관리자용)")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "공통 코드 수정 성공")
  })
  @PatchMapping("/{id}")
  public ResponseEntity<APIResponseDTO<CommonCodeResponseDTO>> updateCode(
          Authentication authentication, @PathVariable Long id, @RequestBody CommonCodeRequestDTO request) {
    request.setCommonCodeId(id);
    return ResponseEntity.ok(APIResponseDTO.<CommonCodeResponseDTO>builder()
            .message("공용 코드 수정 성공")
            .data(commonCodeService.updateCode(authentication.getName(), request))
            .build());
  }
}