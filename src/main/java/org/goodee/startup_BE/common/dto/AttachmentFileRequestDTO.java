package org.goodee.startup_BE.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class AttachmentFileRequestDTO {
	@NotNull(groups = {ValidationGroups.Attachment.Download.class})
	@Positive(groups = {ValidationGroups.Attachment.Download.class})
	private Long fileId;
}
