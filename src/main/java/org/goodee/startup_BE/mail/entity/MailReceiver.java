package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "tbl_mail_receiver", uniqueConstraints = {@UniqueConstraint(columnNames = {"mail_id", "email", "type_id"})})
@Getter
public class MailReceiver {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	@Comment("PK")
	private Long receiverId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mail_id", nullable = false)
	@Comment("수신한 메일")
	private Mail mail;

	@Column(nullable = false)
	@Comment("수신자 이메일")
	private String email;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_id", nullable = false)
	@Comment("수신자 타입")
	private CommonCode type;


	protected MailReceiver() {}

	public static MailReceiver createMailReceiver(Mail mail, String email, CommonCode type) {
		MailReceiver mailReceiver = new MailReceiver();
		mailReceiver.mail = mail;
		mailReceiver.email = email;
		mailReceiver.type = type;
		return mailReceiver;
	}
}
