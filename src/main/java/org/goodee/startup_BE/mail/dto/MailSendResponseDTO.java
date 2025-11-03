package org.goodee.startup_BE.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.mail.entity.Mailbox;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(name = "MailSendResponseDTO", description = "메일 발송 결과 DTO")
public class MailSendResponseDTO {
	@Schema(description = "메일 ID", example = "100")
	private Long mailId;            // 메일 ID
	
	@Schema(description = "제목", example = "회의 자료 공유드립니다")
	private String title;           // 메일 제목
	
	@Schema(description = "발송 시각")
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
