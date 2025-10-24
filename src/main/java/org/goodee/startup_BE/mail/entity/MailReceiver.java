package org.goodee.startup_BE.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.goodee.startup_BE.mail.enums.ReceiverType;

@Entity
@Table(name = "tbl_mail_receiver")
@Getter
public class MailReceiver {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "receiver_id", nullable = false)
	private Long receiverId;
	
	@Column(name = "mail_id", nullable = false)
	private Long mailId;
	
	@Column(nullable = false)
	private String email;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReceiverType type;
	
	@PrePersist
	protected void onPrePersist() {
		type = ReceiverType.TO;
	}
}
