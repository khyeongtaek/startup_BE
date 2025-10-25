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

    @Schema(description = "로그인 아이디", example = "user123")
    private String username;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;
}