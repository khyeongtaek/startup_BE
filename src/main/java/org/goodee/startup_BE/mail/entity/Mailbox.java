package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.mail.enums.MailboxType;

@Entity
@Table(name = "tbl_mailbox")
@Getter
public class Mailbox {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "box_id", nullable = false)
	private Long boxId;
	
	@Column(name = "employee_id", nullable = false)
	private Long employeeId;
	
	@Column(name = "mail_id", nullable = false)
	private Long mailId;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MailboxType type;
	
	@Column(name = "is_read", nullable = false)
	private Boolean isRead;
	
	@Column(name = "deleted_status", nullable = false)
	private Byte deletedStatus;
	
	@PrePersist
	protected void onPrePersist() {
		isRead = false;=
		deletedStatus = 0;
	}
}
