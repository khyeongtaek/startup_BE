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
	
	@NotBlank(groups = {ValidationGroups.Attachment.Upload.class, ValidationGroups.Attachment.List.class})
	private String ownerType;   // 요청한 모듈 타입
	
	@NotNull(groups = {ValidationGroups.Attachment.Upload.class, ValidationGroups.Attachment.List.class})
	@Positive(groups = {ValidationGroups.Attachment.Upload.class, ValidationGroups.Attachment.List.class})
	private Long ownerId;       // 모듈 내 PK
	
	@NotEmpty(groups = {ValidationGroups.Attachment.Upload.class})
	private List<MultipartFile> files;
}
