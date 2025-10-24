package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;

import javax.management.relation.Role;
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
	
	private String content;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "send_type", nullable = false)
	private Role sendType;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "send_status", nullable = false)
	private Role sendStatus;
	
	@Column(name = "send_at")
	private LocalDateTime sendAt;
	
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
	
	@Column(name = "parent_mail_id")
	private Long parentMailId;
	
	@Column(name = "thread_id")
	private Long threadId;
	
	@Column(name = "eml_path")
	private String emlPath;
}
