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
import org.goodee.startup_BE.common.service.CommonCodeServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Common Code API", description = "공통 코드 조회 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/commoncodes")
public class CommonCodeController {

  private final CommonCodeService commonCodeService;
  private final CommonCodeServiceImpl commonCodeServiceImpl;

  @Operation(summary = "특정 prefix 하위 코드 조회 (Root 제외)", description = "특정 prefix를 가진 공통 코드의 하위 코드 목록을 조회. (Root 코드 자체는 제외)")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "코드 조회 성공")
  })
  @GetMapping("/{prefix}")
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getCodes(@PathVariable String prefix) {
    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("["+prefix+"] 코드 조회 성공")
            .data(commonCodeService.getCommonCodeByPrefixWithoutRoot(prefix))
            .build());
  }

  @Operation(summary = "전체 대분류(Prefix) 코드 조회", description = "모든 공통 코드의 대분류(Prefix, Root) 목록을 조회.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "대분류 코드 조회 성공")
  })
  @GetMapping
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getAllPrefix() {

    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("전체 대분류 코드 조회 성공")
            .data(commonCodeService.getAllCodePrefixes())
            .build());
  }


  @Operation(summary = "특정 prefix 하위 코드 조회 (Root 포함)", description = "특정 prefix를 가진 공통 코드의 모든 하위 코드 목록을 조회. (Root 코드 포함)")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "전체 소분류 코드 조회 성공")
  })
  @GetMapping("/prefix/{prefix}")
  public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getAllCodeOnPrefix(@PathVariable String prefix) {
    return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
            .message("전체 소분류 코드 조회 성공")
            .data(commonCodeService.getCommonCodeByPrefix(prefix))
            .build());
  }


  @Operation(summary = "공통 코드 생성", description = "새로운 공통 코드를 시스템에 등록. (관리자용)")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "공통 코드 생성 성공")
  })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<APIResponseDTO<CommonCodeResponseDTO>> createCode(
          Authentication authentication, @RequestBody CommonCodeRequestDTO request) {
    return ResponseEntity.ok(APIResponseDTO.<CommonCodeResponseDTO>builder()
            .message("공용 코드 저장 성공")
            .data(commonCodeService.createCode(authentication.getName(), request))
            .build());
  }

  @Operation(summary = "공통 코드 수정", description = "기존 공통 코드의 정보를 수정. (관리자용)")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "공통 코드 수정 성공")
  })
  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<APIResponseDTO<CommonCodeResponseDTO>> updateCode(
          Authentication authentication, @PathVariable Long id, @RequestBody CommonCodeRequestDTO request) {
    request.setCommonCodeId(id);
    return ResponseEntity.ok(APIResponseDTO.<CommonCodeResponseDTO>builder()
            .message("공용 코드 수정 성공")
            .data(commonCodeService.updateCode(authentication.getName(), request))
            .build());
  }

    @Operation(summary = "휴가 종류 목록 조회", description = "연차/반차 등 휴가 종류 CommonCode 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "휴가 종류 조회 성공")
    })
    @GetMapping("/vacation-types")
    public ResponseEntity<APIResponseDTO<List<CommonCodeResponseDTO>>> getVacationTypes() {

        List<CommonCodeResponseDTO> list = commonCodeService.getVacationTypes();

        return ResponseEntity.ok(APIResponseDTO.<List<CommonCodeResponseDTO>>builder()
                .message("휴가 종류 조회 성공")
                .data(list)
                .build());
    }
}