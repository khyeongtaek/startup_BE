package org.goodee.startup_BE.common.dto;


import jakarta.persistence.Column;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.hibernate.annotations.Comment;

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

    public static CommonCodeResponseDTO toDTO(CommonCode commonCode) {
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
                .build();
    }

}
