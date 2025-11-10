package org.goodee.startup_BE.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.validation.EmployeeValidationGroup;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;


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
    @NotBlank(message = "로그인 아이디는 필수 입력 값입니다.", groups = {ValidationGroups.Create.class})
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.", groups = {EmployeeValidationGroup.ChangePassword.class})
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.", groups = {EmployeeValidationGroup.ChangePassword.class})
    private String password;

    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름은 필수 입력 값입니다.", groups = {ValidationGroups.Create.class})
    private String name;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Schema(description = "연락처", example = "010-1234-5678")
    @NotBlank(message = "연락처는 필수 입력 값입니다.", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String phoneNumber;

    @Schema(description = "입사일", example = "2024-01-01")
    @NotNull(message = "입사일은 필수 입력 값입니다.", groups = {ValidationGroups.Create.class})
    private LocalDate hireDate;

    @Schema(description = "재직 상태 (CommonCode ID)", example = "101")
    @NotNull(message = "재직상태는 필수 입력 값입니다.", groups = {ValidationGroups.Create.class, EmployeeValidationGroup.AdminUpdate.class})
    private Long status;

    @Schema(description = "소속 부서 (CommonCode ID)", example = "201")
    @NotNull(message = "소속부서는 필수 입력 값입니다.", groups = {ValidationGroups.Create.class, EmployeeValidationGroup.AdminUpdate.class})
    private Long department;

    @Schema(description = "직급 (CommonCode ID)", example = "301")
    @NotNull(message = "직급은 필수 입력 값입니다.", groups = {ValidationGroups.Create.class, EmployeeValidationGroup.AdminUpdate.class})
    private Long position;

    @Schema(description = "권한 (CommonCode ID)", example = "901")
    @NotNull(message = "권한은 필수 입력 값입니다.", groups = {ValidationGroups.Create.class, EmployeeValidationGroup.AdminUpdate.class})
    private Long role;

    @Schema(description = "프로필 이미지용 MultipartFile")
    private List<MultipartFile> multipartFile;

    public Employee toEntity(
            CommonCode statusCode,
            CommonCode roleCode,
            CommonCode departmentCode,
            CommonCode positionCode,
            Employee creator
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
                creator
        );
    }


}