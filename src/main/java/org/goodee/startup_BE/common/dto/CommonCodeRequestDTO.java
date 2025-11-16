package org.goodee.startup_BE.common.dto;

import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;


@Getter @Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommonCodeRequestDTO {

    private Long commonCodeId;
    private String code;
    private String codeDescription;
    private String value1;
    private String value2;
    private String value3;
    private Long sortOrder;
    private Long employeeId;
    private Boolean isDisabled;

    public CommonCode toEntity(Employee employee) {
        return CommonCode.createCommonCode(code, codeDescription, value1, value2, value3, sortOrder, employee, isDisabled);
    }


}
