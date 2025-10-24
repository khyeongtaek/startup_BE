package org.goodee.startup_BE.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 공용 첨부파일 엔티티
 */

@Entity
@Table(name = "tbl_file")
@Getter
public class AttachmentFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "file_id", nullable = false)
	private Long fileId;                // 첨부파일 고유 ID
	
	@Column(name = "original_name", nullable = false)
	@Comment("업로드 원본 파일명")
	private String originalName;
	
	@Column(length = 20)
	@Comment("파일 확장자")
	private String ext;
	
	@Column(nullable = false)
	@Comment("파일 사이즈")
	private Long size;

	@Column(name = "storage_path", nullable = false, length = 500)
	@Comment("파일 저장 경로")
	private String storagePath;
	
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "owner_type", nullable = false)
	@Comment("업로드 모듈명")
	private String ownerType;
	
	@Column(name = "owner_id")
	@Comment("모듈 내 고유 ID")
	private Long ownerId;
	
	@PrePersist
	protected void onPrePersist() {
		if(createdAt == null) createdAt = LocalDateTime.now();
	}

	protected AttachmentFile () {};

	public static AttachmentFile createAttachmentFile(String originalName, String ext, Long size, String storagePath, String ownerType, Long ownerId) {
		AttachmentFile attachmentFile = new AttachmentFile();
		attachmentFile.originalName = originalName;
		attachmentFile.ext = ext;
		attachmentFile.size = size;
		attachmentFile.storagePath = storagePath;
		attachmentFile.ownerType = ownerType;
		attachmentFile.ownerId = ownerId;
		return attachmentFile;
	}
}
