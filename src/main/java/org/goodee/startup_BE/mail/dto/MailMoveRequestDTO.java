package org.goodee.startup_BE.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.goodee.startup_BE.common.validation.ValidationGroups;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(name = "MailMoveRequestDTO", description = "메일함 이동/삭제 요청 DTO")
public class MailMoveRequestDTO {
	@NotEmpty
	@Schema(description = "대상 메일함 항목 ID 목록( mailboxId )", example = "[10,11,12]")
	private List<Long> mailIds;
	
	@NotEmpty
	@Schema(description = "이동 대상 메일함 타입 (예: MYBOX, TRASH)", example = "MYBOX")
	private String targetType;
}
