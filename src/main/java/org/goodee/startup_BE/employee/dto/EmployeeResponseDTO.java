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
@Schema(description = "직원 응답 DTO (회원정보 수정/인사관리)")
public class EmployeeResponseDTO {

    @Schema(description = "직원 고유 ID", example = "1")
    private Long employeeId;

    @Schema(description = "로그인 아이디", example = "user123")
    private String username;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Schema(description = "연락처", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "입사일", example = "2024-01-01")
    private LocalDate hireDate;

    @Schema(description = "재직 상태 (CommonCode ID)", example = "101")
    private String status;

    @Schema(description = "프로필 이미지 URL", example = "default_profile.png")
    private String profileImg;

    @Schema(description = "소속 부서", example = "웹개발팀")
    private String department;

    @Schema(description = "직급", example = "사원")
    private String position;

    @Schema(description = "권한", example = "ROLE_ADMIN")
    private String role;

    public Employee toEntity(
            CommonCode statusCode,
            CommonCode roleCode,
            CommonCode departmentCode,
            CommonCode positionCode,
            Employee updater
    ) {

        return Employee.createEmployee(
                this.username,
                this.name,
                this.email,
                this.phoneNumber,
                this.hireDate,
                statusCode,
                roleCode,
                departmentCode,
                positionCode,
                updater
        );
    }

    public static EmployeeResponseDTO toDTO(Employee employee) {
        if( employee == null ) return null;
        return EmployeeResponseDTO.builder()
                .employeeId(employee.getEmployeeId())
                .username(employee.getUsername())
                .name(employee.getName())
                .email(employee.getEmail())
                .phoneNumber(employee.getPhoneNumber())
                .hireDate(employee.getHireDate())
                .status(employee.getStatus().getValue1())
                .profileImg(employee.getProfileImg())
                .department(employee.getDepartment().getValue1())
                .position(employee.getPosition().getValue1())
                .role(employee.getRole().getValue1())
                .build();
    }




}