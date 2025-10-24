package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_mail")
@Getter
public class Mail {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "mail_id", nullable = false)
	private Long mailId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_id", nullable = false)
	private Employee sender;
	
	@Column(nullable = false)
	private String title;
	
	@Column(columnDefinition = "LONGTEXT")
	private String content;
	
	@Column(name = "send_at", nullable = false)
	private LocalDateTime sendAt;
	
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_mail_id")
	@Comment("부모 메일")
	private Mail parentMail;
	
	@Column(name = "thread_id")
	private Long threadId;
	
	@Column(name = "eml_path", length = 500)
	private String emlPath;

	@OneToMany(mappedBy = "parentMail")
	@OrderBy("createdAt ASC")
	@Comment("회신 스레드에 사용 ")
	private List<Mail> replies = new ArrayList<>();
	
	
	@PrePersist
	protected void onPrePersist() {
		if(createdAt == null) createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onPreUpdate() {
		updatedAt = LocalDateTime.now();
	}

	protected Mail() {}

	// 기본 메일 작성
	public static Mail createBasicMail(Employee sender, String title, String content, LocalDateTime sendAt, String emlPath) {
		Mail mail = new Mail();
		mail.sender = sender;
		mail.title = title;
		mail.content = content;
		mail.sendAt = sendAt;
		mail.emlPath = emlPath;
		return mail;
	}

	// 회신 메일 작성
	public static Mail createReplyMail(Employee sender, String title, String content, LocalDateTime sendAt, Mail parentMail, Long threadId, String emlPath) {
		Mail mail = createBasicMail(sender, title, content, sendAt, emlPath);
		mail.parentMail = parentMail;
		mail.threadId = threadId;
		return mail;
	}
}
