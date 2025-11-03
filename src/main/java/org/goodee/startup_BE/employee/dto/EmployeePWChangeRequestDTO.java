package org.goodee.startup_BE.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "직원 비밀번호 변경 요청 DTO")
public class EmployeePWChangeRequestDTO {

    @Schema(description = "현재 비밀번호", example = "currentPassword123")
    @NotBlank(message = "현재 비밀번호는 필수 입력 값입니다.")
    private String currentPassword;

    @Schema(description = "새 비밀번호", example = "newPassword123!")
    @NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    private String newPassword;
}
