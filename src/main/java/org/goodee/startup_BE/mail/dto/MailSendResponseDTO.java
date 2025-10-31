package org.goodee.startup_BE.mail.dto;

import lombok.*;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.mail.entity.Mailbox;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MailSendResponseDTO {
	private Long mailId;            // 메일 ID
	private String title;           // 메일 제목
	private LocalDateTime sendAt;   // 발송 시각
	
	// 화면에서 인원수/파일첨부개수 를 표시하기 위한 필드값(선택사항)
	private int toCount;
	private int ccCount;
	private int bccCount;
	private int attachmentCount;
	
	private String emlPath;
	
	public static MailSendResponseDTO toDTO(Mail mail, int toCount, int ccCount, int bccCount, int attachmentCount) {
		return MailSendResponseDTO.builder()
			       .mailId(mail.getMailId())
			       .title(mail.getTitle())
			       .sendAt(mail.getSendAt())
			       .toCount(toCount)
			       .ccCount(ccCount)
			       .bccCount(bccCount)
			       .attachmentCount(attachmentCount)
			       .emlPath(mail.getEmlPath())
			       .build();
	}
}
