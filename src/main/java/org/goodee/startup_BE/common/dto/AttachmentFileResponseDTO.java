package org.goodee.startup_BE.common.dto;

import lombok.*;
import org.goodee.startup_BE.common.entity.AttachmentFile;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class AttachmentFileResponseDTO {
	private Long fileId;
	private String originalName;
	private String ext;
	private Long size;
	private String storagePath;         // 상대경로
	private String mimeType;            // image/png, application/pdf
	private LocalDateTime createdAt;
	
	public static AttachmentFileResponseDTO toDTO(AttachmentFile file) {
		return AttachmentFileResponseDTO.builder()
			       .fileId(file.getFileId())
			       .originalName(file.getOriginalName())
			       .ext(file.getExt())
			       .size(file.getSize())
			       .storagePath(file.getStoragePath())
			       .mimeType(file.getMimeType())
			       .createdAt(file.getCreatedAt())
			       .build();
	}
}
