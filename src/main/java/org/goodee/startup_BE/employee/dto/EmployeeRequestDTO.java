package org.goodee.startup_BE.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDate;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(description = "직원 요청 DTO (회원가입/로그인/수정 시 사용)")
public class EmployeeRequestDTO {

    @Schema(description = "직원 고유 ID (수정 시 사용)", example = "1")
    private Long employeeId;

    @Schema(description = "로그인 아이디", example = "user123")
    private String username;

    @Schema(description = "비밀번호", example = "password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Schema(description = "연락처", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "입사일", example = "2024-01-01")
    private LocalDate hireDate;

    @Schema(description = "재직 상태 (CommonCode ID)", example = "101")
    private Long status;

    @Schema(description = "프로필 이미지 URL", example = "default_profile.png")
    private String profileImg;

    @Schema(description = "소속 부서 (CommonCode ID)", example = "201")
    private Long department;

    @Schema(description = "직급 (CommonCode ID)", example = "301")
    private Long position;

    @Schema(description = "권한 (CommonCode ID)", example = "901")
    private Long role;

    public Employee toEntity(
            CommonCode statusCode,
            CommonCode roleCode,
            CommonCode departmentCode,
            CommonCode positionCode,
            Employee creater
    ) {

        return Employee.createEmployee(
                this.username,
                this.name,
                this.email,
                this.phoneNumber,
                this.hireDate,
                statusCode,
                this.profileImg,
                roleCode,
                departmentCode,
                positionCode,
                creater
        );
    }


}