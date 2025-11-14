package org.goodee.startup_BE.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.employee.dto.EmployeePWChangeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
import org.goodee.startup_BE.employee.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication API", description = "인증 (회원가입, 로그인, 로그아웃) API")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    // 회원가입
    @PostMapping("/signup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "직원 등록 (회원가입)", description = "관리자가 새로운 직원을 등록")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "직원 등록 성공",
                    content = @Content(schema = @Schema(implementation = APIResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 이메일 중복)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자가 아님)")
    })
    public ResponseEntity<APIResponseDTO<EmployeeResponseDTO>> register(
            Authentication authentication, // Spring Security가 주입하는 인증된 사용자 정보
            @Validated(ValidationGroups.Create.class)
            @RequestBody EmployeeRequestDTO employeeRequestDTO // 등록할 직원 정보
    ) {
        return ResponseEntity.ok(
                authenticationService.signup(authentication, employeeRequestDTO));
    }

    @PostMapping("/synchr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponseDTO<Map<String, Object>>> syncHR(
            @Parameter(hidden = true) Authentication authentication,
            @RequestParam("multipartFile") MultipartFile multipartFile
    ) {
        return ResponseEntity.ok(APIResponseDTO.<Map<String, Object>>builder()
                .message("인사정보 연동 성공")
                .data(authenticationService.syncHR(authentication, multipartFile))
                .build());
    }

    // 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디(username)와 비밀번호로 로그인하여 JWT 토큰을 발급. (로그인 시 클라이언트의 IP 주소와 User-Agent 정보가 기록)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = APIResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (아이디 또는 비밀번호 오류)")
    })
    public ResponseEntity<EmployeeResponseDTO> login(
            @RequestBody EmployeeRequestDTO employeeRequestDTO
            , HttpServletRequest request // IP, User-Agent 추출을 위해 HttpServletRequest 주입
            , HttpServletResponse response
    ) {

        // IP 주소와 User-Agent 추출
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        Map<String, Object> loginResult = authenticationService
                .login(
                        employeeRequestDTO
                        , ipAddress
                        , userAgent
                );

        addTokensToCookies(response, loginResult.get("accessToken").toString(), loginResult.get("refreshToken").toString());

        return ResponseEntity.ok((EmployeeResponseDTO) loginResult.get("employee"));


    }


    @Operation(summary = "사용자 비밀번호 변경",
            description = "로그인한 사용자 본인의 비밀번호를 변경. (현재 비밀번호와 새 비밀번호 필요)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "현재 비밀번호와 새 비밀번호",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmployeePWChangeRequestDTO.class))
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "인증되지 않았거나 현재 비밀번호가 틀림", content = @Content)
    })
    @PatchMapping("/updateEmployeePassword")
    public ResponseEntity<APIResponseDTO<EmployeeResponseDTO>> updateEmployeePassword(
            @Parameter(hidden = true) Authentication authentication,
            @Validated @RequestBody EmployeePWChangeRequestDTO request
    ) {
        return ResponseEntity.ok(APIResponseDTO.<EmployeeResponseDTO>builder()
                .message("비밀번호 변경 성공")
                .data(authenticationService.updateEmployeePassword(authentication.getName(), request))
                .build());
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    @Operation(summary = "Access Token 갱신", description = "유효한 Refresh Token을 'Authorization: Bearer <token>' 헤더에 담아 전송하면 새로운 Access Token과 기존 Refresh Token을 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = APIResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않거나 만료된 Refresh Token)")
    })
    public ResponseEntity<EmployeeResponseDTO> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 쿠키에서 Refresh Token 추출
        String refreshToken = extractTokenFromCookies(request, "refreshToken");
        if (refreshToken == null) {
            throw new BadCredentialsException("Refresh Token 쿠키를 찾을 수 없습니다.");
        }

        Map<String, Object> refreshResult = authenticationService.refreshToken(refreshToken);

        addTokensToCookies(response, refreshResult.get("accessToken").toString(), refreshResult.get("refreshToken").toString());

        return ResponseEntity.ok((EmployeeResponseDTO) refreshResult.get("employee"));

    }

    // 로그아웃
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "서버에서 HttpOnly 쿠키를 만료시켜 로그아웃 처리")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<APIResponseDTO<String>> logout(HttpServletResponse response) {
        // 쿠키를 만료시키는 헬퍼 메서드 호출
        clearCookies(response);

        // 성공 응답 반환
        return ResponseEntity.ok(
                APIResponseDTO.<String>builder()
                        .message("로그아웃 되었습니다.")
                        .data("Logout successful")
                        .build()
        );
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

    // HttpServletRequest에서 특정 이름의 쿠키 값을 추출하는 헬퍼 메서드
    private String extractTokenFromCookies(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // 토큰을 HttpOnly 쿠키로 변환하여 Response에 추가하는 헬퍼 메서드
    private void addTokensToCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Access Token 쿠키 생성
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true); // JavaScript에서 접근 불가
        accessTokenCookie.setPath("/"); // 모든 경로에서 쿠키 사용
        // TODO: 배포 환경(HTTPS)에서는 setSecure(true) 설정 필요
        // accessTokenCookie.setSecure(true);

        // Refresh Token 쿠키 생성
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/"); // /api/auth/refresh 경로에서만 사용하려면 "/api/auth/refresh"로 설정
        // TODO: 배포 환경(HTTPS)에서는 setSecure(true) 설정 필요
        // refreshTokenCookie.setSecure(true);

        // 응답에 쿠키 추가
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    // 토큰을 HttpOnly 쿠키에서 삭제(만료)하는 헬퍼 메서드
    private void clearCookies(HttpServletResponse response) {
        // Access Token 쿠키 만료
        Cookie accessTokenCookie = new Cookie("accessToken", null); // value를 null로 설정
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // 만료 시간을 0으로 설정
        // TODO: 배포 환경(HTTPS)에서는 setSecure(true) 설정 필요
        // accessTokenCookie.setSecure(true);

        // Refresh Token 쿠키 만료
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        // TODO: 배포 환경(HTTPS)에서는 setSecure(true) 설정 필요
        // refreshTokenCookie.setSecure(true);

        // 응답에 만료된 쿠키 추가
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }
}