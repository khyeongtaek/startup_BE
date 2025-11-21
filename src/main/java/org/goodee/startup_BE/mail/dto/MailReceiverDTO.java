package org.goodee.startup_BE.mail.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MailReceiverDTO {
	private Long employeeId;
	private String name;
	private String email;
	private String profileImg;
	private String position;
	private String department;
}
