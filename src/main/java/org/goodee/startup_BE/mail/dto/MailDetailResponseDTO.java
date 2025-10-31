package org.goodee.startup_BE.mail.dto;

import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public class MailDetailResponseDTO {
	private Long mailId;
	private String title;
	private String content;
	private LocalDateTime sendAt;
	private String emlPath;
	
	// 발신자 정보
	private Long senderId;
	private String senderName;
	private String senderEmail;
	
	// 수신자 정보
	private List<String> to;
	private List<String> cc;
	private List<String> bcc;
	
	// 메일함 정보
	private Long boxId;
	private String mailboxType;
	private Boolean isRead;
	private Byte deletedStatus;
	
	// 첨부
	private List<AttachmentFileResponseDTO> attachments;
	private int attachmentCount;
}
