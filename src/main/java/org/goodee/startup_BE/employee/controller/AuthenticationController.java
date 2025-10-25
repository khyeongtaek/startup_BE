package org.goodee.startup_BE.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.employee.dto.AuthenticationResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication API", description = "인증 (회원가입, 로그인, 로그아웃) API")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    // 회원가입
    @PostMapping("/signup")
    @Operation(summary = "직원 등록 (회원가입)", description = "관리자가 새로운 직원을 등록합니다. (관리자 권한 필요)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "직원 등록 성공",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 이메일 중복)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자가 아님)")
    })
    // 'bearerAuth'는 SpringDoc OpenAPI 설정에 정의된 SecurityScheme 이름이어야 함.
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthenticationResponseDTO> register(
            Authentication authentication // Spring Security가 주입하는 인증된 사용자 정보
            , @RequestBody EmployeeRequestDTO request // 등록할 직원 정보
    ) {
        return ResponseEntity.ok(
                authenticationService.signup(authentication, request));
    }

    // 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디(username)와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (아이디 또는 비밀번호 오류)")
    })
    public ResponseEntity<AuthenticationResponseDTO> authenticate(
            @RequestBody EmployeeRequestDTO request
    ) {
        return ResponseEntity.ok(authenticationService.login(request));
    }
}