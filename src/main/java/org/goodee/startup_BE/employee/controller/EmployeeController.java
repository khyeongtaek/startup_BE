package org.goodee.startup_BE.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
import org.goodee.startup_BE.employee.service.EmployeeService;
import org.goodee.startup_BE.employee.validation.EmployeeValidationGroup;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Employee API", description = "사원 정보 관리 API")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;


    @Operation(summary = "로그인한 본인 정보 조회", description = "username 을 기준으로 로그인한  사원의 상세 정보를 조회.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사원 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = "해당 사원을 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/myInfo")
    public ResponseEntity<APIResponseDTO<EmployeeResponseDTO>> getMyInfo(
            Authentication authentication // Spring Security가 주입하는 인증된 사용자 정보
    ) {
        return ResponseEntity.ok(APIResponseDTO.<EmployeeResponseDTO>builder()
                .message("사원 정보 조회 성공")
                .data(employeeService.getEmployee(authentication.getName()))
                .build());
    }

    @Operation(summary = "특정 사원 정보 조회", description = "사원 ID를 기준으로 특정 사원의 상세 정보를 조회.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사원 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = "해당 사원을 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<APIResponseDTO<EmployeeResponseDTO>> getEmployee(
            @Parameter(description = "조회할 사원의 ID (employee_id)", required = true, example = "1")
            @PathVariable("id") Long employeeId
    ) {
        return ResponseEntity.ok(APIResponseDTO.<EmployeeResponseDTO>builder()
                .message("사원 정보 조회 성공")
                .data(employeeService.getEmployee(employeeId))
                .build());
    }

    @Operation(summary = "특정 부서 소속원 목록 조회", description = "부서 ID를 기준으로 해당 부서에 소속된 모든 사원 목록을 조회.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "부서원 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<APIResponseDTO<List<EmployeeResponseDTO>>> getDepartmentMembers(
            @Parameter(description = "조회할 부서 ID", required = true, example = "1")
            @PathVariable("departmentId") Long departmentId
    ) {
        return ResponseEntity.ok(APIResponseDTO.<List<EmployeeResponseDTO>>builder()
                .message("부서원 목록 조회 성공")
                .data(employeeService.getDepartmentMembers(departmentId))
                .build());
    }

    @Operation(summary = "사용자 본인 정보 수정",
            description = "로그인한 사용자 본인의 정보(예: 전화번호)를 수정.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 사용자 정보.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmployeeRequestDTO.class))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "개인 정보 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않았거나 수정 권한 없음", content = @Content)
    })
    @PatchMapping("/updateEmployeeByUser")
    public ResponseEntity<APIResponseDTO<EmployeeResponseDTO>> updateEmployeeByUser(
            @Parameter(hidden = true) Authentication authentication,
            @Validated(ValidationGroups.Update.class)
            @RequestBody EmployeeRequestDTO request
    ) {
        return ResponseEntity.ok(APIResponseDTO.<EmployeeResponseDTO>builder()
                .message("개인 정보 수정 성공")
                .data(employeeService.updateEmployeeByUser(authentication.getName(), request))
                .build());
    }


    @Operation(summary = "(관리자) 사원 인사 정보 수정",
            description = "관리자가 대상 사원의 인사 정보(직위, 부서, 역할, 상태)를 수정. (ADMIN 권한 필요)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 사원 정보. 'username' 필드에 대상 사원의 ID를 포함.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmployeeRequestDTO.class))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사원 정보 수정(관리자) 성공"),
            @ApiResponse(responseCode = "400", description = "대상 사원 또는 코드 정보를 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 (ADMIN 아님)", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/updateEmployeeByAdmin")
    public ResponseEntity<APIResponseDTO<EmployeeResponseDTO>> updateEmployeeByAdmin(
            Authentication authentication,
            @Validated(EmployeeValidationGroup.AdminUpdate.class)
            @RequestBody EmployeeRequestDTO request,
            @PathVariable("id") Long employeeId
    ) {
        request.setEmployeeId(employeeId);
        return ResponseEntity.ok(APIResponseDTO.<EmployeeResponseDTO>builder()
                .message("사원 정보 수정(관리자) 성공")
                .data(employeeService.updateEmployeeByAdmin(authentication.getName(), request))
                .build());
    }

    @Operation(summary = "(관리자) 사원 비밀번호 초기화",
            description = "관리자가 대상 사원의 비밀번호를 초기화. (ADMIN 권한 필요)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "비밀번호를 초기화할 대상 사원의 'username'이 담긴 DTO.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmployeeRequestDTO.class))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사원 비밀번호 초기화 성공"),
            @ApiResponse(responseCode = "400", description = "대상 사원을 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 (ADMIN 아님)", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/initPassword/{id}")
    public ResponseEntity<APIResponseDTO<EmployeeResponseDTO>> initPassword(
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody EmployeeRequestDTO request,
            @PathVariable("id") Long employeeId
    ) {
        request.setEmployeeId(employeeId);
        return ResponseEntity.ok(APIResponseDTO.<EmployeeResponseDTO>builder()
                .message("사원 비밀번호 초기화 성공")
                .data(employeeService.initPassword(authentication.getName(), request))
                .build());
    }

}