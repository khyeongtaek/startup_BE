package org.goodee.startup_BE.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

// 인증 응답 DTO (로그인 성공 시)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(description = "인증 응답 DTO (로그인/회원가입 성공 시)")
public class AuthenticationResponseDTO {

    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "JWT Refresh Token (추후 구현 예정)", example = "null")
    private String refreshToken;

    @Schema(description = "인증된 사용자(직원) 상세 정보")
    private EmployeeResponseDTO user;
}