package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.mail.enums.ReceiverType;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "tbl_mail_receiver", uniqueConstraints = {@UniqueConstraint(columnNames = {"mail_id", "email", "type"})})
@Getter
public class MailReceiver {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "receiver_id", nullable = false)
	private Long receiverId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mail_id", nullable = false)
	private Mail mail;

	@Column(nullable = false)
	@Comment("수신자 이메일")
	private String email;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReceiverType type;


	@PrePersist
	protected void onPrePersist() {
		if(type == null) type = ReceiverType.TO;
	}

	protected MailReceiver() {}

	public static MailReceiver createMailReceiver(Mail mail, String email, ReceiverType type) {
		MailReceiver mailReceiver = new MailReceiver();
		mailReceiver.mail = mail;
		mailReceiver.email = email;
		mailReceiver.type = type;
		return mailReceiver;
	}
}
