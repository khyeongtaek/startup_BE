package org.goodee.startup_BE.work_log.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;

import java.util.List;

@AllArgsConstructor
@Getter
public class WorkLogCodeListDTO {
	private List<CommonCodeResponseDTO> workTypes;
	private List<CommonCodeResponseDTO> workOptions;
}
