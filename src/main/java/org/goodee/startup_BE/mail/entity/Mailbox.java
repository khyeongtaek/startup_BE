package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.mail.enums.MailboxType;

@Entity
@Table(name = "tbl_mailbox", uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id", "mail_id", "type"})})
@Getter
public class Mailbox {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "box_id", nullable = false)
	private Long boxId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mail_id", nullable = false)
	private Mail mail;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MailboxType type;
	
	@Column(name = "is_read", nullable = false)
	private Boolean isRead;
	
	@Column(name = "deleted_status", nullable = false)
	private Byte deletedStatus;
	
	@PrePersist
	protected void onPrePersist() {
		if(isRead == null) isRead = false;
		if(deletedStatus == null) deletedStatus = 0;
	}

	protected Mailbox() {}

	public static Mailbox createMailbox(Employee employee, Mail mail, MailboxType type, Boolean isRead, Byte deletedStatus) {
		Mailbox mailbox = new Mailbox();
		mailbox.employee = employee;
		mailbox.mail = mail;
		mailbox.type = type;
		mailbox.isRead = isRead;
		mailbox.deletedStatus = deletedStatus;
		return mailbox;
	}

	// 휴지통 이동
	public void moveToTrash() {
		this.deletedStatus = 1;
	}

	// 휴지통에서 삭제 (소프트 삭제)
	public void deleteFromTrash() {
		this.deletedStatus = 2;
	}
}
