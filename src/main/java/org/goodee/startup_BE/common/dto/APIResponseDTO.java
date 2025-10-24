package org.goodee.startup_BE.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class APIResponseDTO<T> {
    private String message;
    private T data;
}
