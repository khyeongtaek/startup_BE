package org.goodee.startup_BE.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.dto.AuthenticationResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
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
    public ResponseEntity<APIResponseDTO<EmployeeResponseDTO>> register(
            Authentication authentication // Spring Security가 주입하는 인증된 사용자 정보
            , @RequestBody EmployeeRequestDTO employeeRequestDTO // 등록할 직원 정보
    ) {
        return ResponseEntity.ok(
                authenticationService.signup(authentication, employeeRequestDTO));
    }

    // 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디(username)와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다. 로그인 시 클라이언트의 IP 주소와 User-Agent 정보가 기록됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (아이디 또는 비밀번호 오류)")
    })
    public ResponseEntity<AuthenticationResponseDTO> login(
            @RequestBody EmployeeRequestDTO employeeRequestDTO
            , HttpServletRequest request // IP, User-Agent 추출을 위해 HttpServletRequest 주입
    ) {

        // IP 주소와 User-Agent 추출
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        return ResponseEntity
                .ok(authenticationService
                        .login(
                                employeeRequestDTO
                                , ipAddress
                                , userAgent
                        ));
    }


    // 프록시 환경(Nginx 등)을 고려한 클라이언트 IP 추출 헬퍼 메서드
    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty() && !"unknown".equalsIgnoreCase(xfHeader)) {
            // X-Forwarded-For 헤더는 콤마로 구분된 IP 리스트일 수 있음. 첫 번째 IP를 사용.
            return xfHeader.split(",")[0].trim();
        }

        String proxyClientIP = request.getHeader("Proxy-Client-IP");
        if (proxyClientIP != null && !proxyClientIP.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIP)) {
            return proxyClientIP;
        }

        String wlProxyClientIP = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIP != null && !wlProxyClientIP.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIP)) {
            return wlProxyClientIP;
        }

        String httpClientIP = request.getHeader("HTTP_CLIENT_IP");
        if (httpClientIP != null && !httpClientIP.isEmpty() && !"unknown".equalsIgnoreCase(httpClientIP)) {
            return httpClientIP;
        }

        String httpXForwardedFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (httpXForwardedFor != null && !httpXForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(httpXForwardedFor)) {
            return httpXForwardedFor;
        }

        // 모든 헤더에 IP가 없는 경우, 최후의 수단으로 getRemoteAddr() 사용
        return request.getRemoteAddr();
    }
}