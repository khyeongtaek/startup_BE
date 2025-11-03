package org.goodee.startup_BE.mail.dto;

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
public class MailMoveRequestDTO {
	@NotEmpty(groups = {ValidationGroups.Mail.Move.class, ValidationGroups.Mail.Delete.class})
	private List<Long> mailIds;
	
	@NotEmpty(groups = {ValidationGroups.Mail.Move.class})
	private String targetType;
}
