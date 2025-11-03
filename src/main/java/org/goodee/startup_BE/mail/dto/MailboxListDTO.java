package org.goodee.startup_BE.mail.dto;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class MailboxListDTO {
	private Long boxId;
	private Long mailId;
	private String senderName;
	private String title;
	private LocalDateTime receivedAt;
	private Boolean isRead;
}
