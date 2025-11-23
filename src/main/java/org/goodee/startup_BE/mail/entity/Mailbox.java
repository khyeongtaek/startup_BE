package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "tbl_mailbox", uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id", "mail_id", "type_id"})})
@Getter
public class Mailbox {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	@Comment("PK")
	private Long boxId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id")
	@Comment("사용자 ID")
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private Employee employee;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mail_id", nullable = false)
	@Comment("메일 ID")
	private Mail mail;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_id", nullable = false)
	@Comment("메일함 타입")
	private CommonCode typeId;
	
	@Column(nullable = false)
	@Comment("읽음 여부")
	private Boolean isRead;
	
	@Column(nullable = false)
	@Comment("삭제 단계 - 0 : 삭제 X, 1 : 휴지통 이동, 2 : 휴지통에서 삭제(소프트삭제, 조회 X")
	private Byte deletedStatus;
	
	@PrePersist
	protected void onPrePersist() {
		if(isRead == null) isRead = false;
		if(deletedStatus == null) deletedStatus = 0;
	}

	protected Mailbox() {}

	public static Mailbox createMailbox(Employee employee, Mail mail, CommonCode type, Boolean isRead, Byte deletedStatus) {
		Mailbox mailbox = new Mailbox();
		mailbox.employee = employee;
		mailbox.mail = mail;
		mailbox.typeId = type;
		mailbox.isRead = isRead;
		mailbox.deletedStatus = deletedStatus;
		return mailbox;
	}

	// 메일함 이동
	public void moveMail(CommonCode type) {
		String value = type.getValue1();
		
		// 휴지통 이동: typeId 는 그대로 두고, deletedStatus 만 1로 설정
		if ("TRASH".equals(value)) {
			this.deletedStatus = 1;  // 휴지통
			this.isRead = false;
			return;
		}
		
		// MYBOX / INBOX / SENT 등 일반 이동
		this.typeId = type;
		this.deletedStatus = 0;  // 일반 메일함
		this.isRead = false;
	}

	// 휴지통에서 삭제 (소프트 삭제)
	public void deleteFromTrash() {
		this.deletedStatus = 2;
	}
	
	public void markAsRead() {
		this.isRead = true;
	}
}
