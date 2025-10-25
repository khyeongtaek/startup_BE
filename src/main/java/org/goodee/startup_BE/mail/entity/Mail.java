package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.employee.entity.Employee;
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
	@Column(nullable = false)
	@Comment("PK")
	private Long mailId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	@Comment("발신자")
	private Employee employee;
	
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	@Comment("메일 제목")
	private String title;
	
	@Column(columnDefinition = "LONGTEXT")
	@Comment("메일 본문")
	private String content;
	
	@Column(nullable = false)
	@Comment("메일 발송 시각")
	private LocalDateTime sendAt;
	
	@Column(nullable = false, updatable = false)
	@Comment("메일 생성 시각")
	private LocalDateTime createdAt;
	
	@Column(nullable = false)
	@Comment("메일 수정 시각")
	private LocalDateTime updatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_mail_id")
	@Comment("회신 시 부모 메일")
	private Mail parentMail;
	
	@Comment("스레드 묶음 번호 - 회신 시 사용")
	private Long threadId;
	
	@Column(length = 500)
	@Comment("eml 파일 저장 경로")
	private String emlPath;

	@OneToMany(mappedBy = "parentMail")
	@OrderBy("createdAt ASC")
	@Comment("회신 스레드에 사용")
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
	public static Mail createBasicMail(Employee employee, String title, String content, LocalDateTime sendAt, String emlPath) {
		Mail mail = new Mail();
		mail.employee = employee;
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
