package org.goodee.startup_BE.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * 공용 첨부파일 엔티티
 */

@Entity
@Table(name = "tbl_file")
@Where(clause = "is_deleted = false")
@Getter
public class AttachmentFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	@Comment("PK")
	private Long fileId;
	
	@Column(nullable = false)
	@Comment("업로드 원본 파일명")
	private String originalName;
	
	@Column(length = 20)
	@Comment("파일 확장자")
	private String ext;
	
	@Column(nullable = false)
	@Comment("파일 사이즈")
	private Long size;

	@Column(nullable = false, length = 500, unique = true)
	@Comment("파일 저장 경로")
	private String storagePath;
	
	@Column(name = "mime_type", length = 100)
	@Comment("mime 타입")
	private String mimeType;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "is_deleted", nullable = false)
	@Comment("삭제여부 - true:삭제, false:삭제X (소프트삭제)")
	private Boolean isDeleted;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_type_id", nullable = false)
	@Comment("업로드 모듈명")
	private CommonCode ownerType;		// 공용 테이블에 모듈 추가 필요, OwnerType enum값 추가 필요
	
	@Comment("모듈 내 고유 ID")
	private Long ownerId;


	@PrePersist
	protected void onPrePersist() {
		if(createdAt == null) createdAt = LocalDateTime.now();
		if(isDeleted == null) isDeleted = false;
	}

	protected AttachmentFile() {};

	public static AttachmentFile createAttachmentFile(String originalName, String ext, Long size, String storagePath, String mimeType, CommonCode ownerType, Long ownerId) {
		AttachmentFile attachmentFile = new AttachmentFile();
		attachmentFile.originalName = originalName;
		attachmentFile.ext = ext;
		attachmentFile.size = size;
		attachmentFile.storagePath = storagePath;
		attachmentFile.mimeType = mimeType;
		attachmentFile.ownerType = ownerType;
		attachmentFile.ownerId = ownerId;
		return attachmentFile;
	}
	
	public void deleteAttachmentFile() {
		this.isDeleted = true;
	}
}
