package org.goodee.startup_BE.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.common.enums.OwnerType;
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
	private String originalName;        // 업로드 원본 파일명
	
	@Column(length = 20)
	private String ext;                 // 파일 확장자
	
	@Column(nullable = false)
	private Long size;
	
	@Column(name = "storage_path", nullable = false, length = 500)
	private String storagePath;
	
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "owner_type", nullable = false)
	@Comment("업로드 모듈명")
	private OwnerType ownerType;
	
	@Column(name = "owner_id")
	@Comment("모듈 내 고유 ID")
	private Long ownerId;
	
	@PrePersist
	protected void onPrePersist() {
		createdAt = LocalDateTime.now();
	}
}
