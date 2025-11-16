package org.goodee.startup_BE.common.dto;


import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommonCodeResponseDTO {

    private Long commonCodeId;
    private String code;
    private String codeDescription;
    private String value1;
    private String value2;
    private String value3;
    private Long sortOrder;
//    private Long employeeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDisabled;
    private String creator;
    private String updater;

    public static CommonCodeResponseDTO toDTO(CommonCode commonCode) {

        Employee creatorEntity = commonCode.getCreator();
        Employee updaterEntity = commonCode.getUpdater();

        String creator = (creatorEntity != null) ? creatorEntity.getUsername() : null;
        String updater = (updaterEntity != null) ? updaterEntity.getUsername() : null;
        return CommonCodeResponseDTO.builder()
                .commonCodeId(commonCode.getCommonCodeId())
                .code(commonCode.getCode())
                .codeDescription(commonCode.getCodeDescription())
                .value1(commonCode.getValue1())
                .value2(commonCode.getValue2())
                .value3(commonCode.getValue3())
                .sortOrder(commonCode.getSortOrder())
                .createdAt(commonCode.getCreatedAt())
                .updatedAt(commonCode.getUpdatedAt())
                .isDisabled(commonCode.getIsDisabled())
                .creator(creator)
                .updater(updater)
                .build();
    }

}
