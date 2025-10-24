package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.mail.enums.SendStatus;
import org.goodee.startup_BE.mail.enums.SendType;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_mail")
@Getter
public class Mail {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "mail_id", nullable = false)
	private Long mailID;
	
	@Column(name = "sender_id", nullable = false)
	private Long senderId;
	
	@Column(nullable = false)
	private String title;
	
	@Column(columnDefinition = "LONGTEXT")
	private String content;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "send_type", nullable = false)
	private SendType sendType;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "send_status", nullable = false)
	private SendStatus sendStatus;
	
	@Column(name = "send_at")
	private LocalDateTime sendAt;
	
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
	
	@Column(name = "parent_mail_id")
	private Long parentMailId;
	
	@Column(name = "thread_id")
	private Long threadId;
	
	@Column(name = "eml_path", length = 500)
	private String emlPath;
	
	
	@PrePersist
	protected void onPrePersist() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
		sendType = SendType.NORMAL;
		sendStatus = SendStatus.WAIT;
	}
	
	@PreUpdate
	protected void onPreUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
