package org.goodee.startup_BE.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.common.entity.AttachmentFile;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(name = "AttachmentFileResponseDTO", description = "첨부파일 응답 DTO")
public class AttachmentFileResponseDTO {
	@Schema(description = "파일 ID", example = "101")
	private Long fileId;
	
	@Schema(description = "업로드 원본 파일명", example = "회의록_2025-11-06.pdf")
	private String originalName;
	
	@Schema(description = "파일 확장자", example = "pdf")
	private String ext;
	
	@Schema(description = "파일 크기(bytes)", example = "2048576")
	private Long size;
	
	@Schema(description = "저장 상대 경로", example = "MAIL/2025/11/06/ab12cd34ef56.pdf")
	private String storagePath;         // 상대경로
	
	@Schema(description = "MIME 타입", example = "application/pdf")
	private String mimeType;            // image/png, application/pdf
	
	@Schema(description = "업로드(생성) 시각", example = "2025-11-06T13:45:12")
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
