package org.goodee.startup_BE.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(name = "MailboxListDTO", description = "메일함 목록 아이템 DTO")
public class MailboxListDTO {
	@Schema(description = "메일함 항목 ID (mailboxId)", example = "10")
	private Long boxId;
	
	@Schema(description = "메일 ID", example = "100")
	private Long mailId;
	
	@Schema(description = "보낸 사람", example = "홍길동")
	private String senderName;
	private String senderPosition;
	private String senderDepartment;
	private String senderProfileImg;
	private String senderEmail;
	
	@Schema(description = "제목", example = "회의 자료 공유드립니다")
	private String title;
	
	@Schema(description = "수신/도착 시각")
	private LocalDateTime receivedAt;
	
	@Schema(description = "읽음 여부")
	private Boolean isRead;
	
	@Schema(description = "수신자들")
	private List<MailReceiverDTO> receivers;
}
