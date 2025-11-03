package org.goodee.startup_BE.mail.dto;

import lombok.*;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.mail.entity.Mailbox;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(exclude = "attachmentFiles")
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
	
	// 카운팅을 위한 필드
	private int toCount;
	private int ccCount;
	private int bccCount;
	private int attachmentCount;
	
	public static MailDetailResponseDTO toDTO(
		Mail mail, List<String> toList, List<String> ccList, List<String> bccList,
		Mailbox mailbox, List<AttachmentFileResponseDTO> attachmentFiles
	) {
		List<String> safeTo  = (toList  == null) ? java.util.Collections.emptyList() : toList;
		List<String> safeCc  = (ccList  == null) ? java.util.Collections.emptyList() : ccList;
		List<String> safeBcc = (bccList == null) ? java.util.Collections.emptyList() : bccList;
		List<AttachmentFileResponseDTO> safeFiles = (attachmentFiles == null) ? java.util.Collections.emptyList() : attachmentFiles;
		
		return MailDetailResponseDTO.builder()
			       .mailId(mail.getMailId())
			       .title(mail.getTitle())
			       .content(mail.getContent())
			       .sendAt(mail.getSendAt())
			       .emlPath(mail.getEmlPath())
			       .senderId(mail.getEmployee().getEmployeeId())
			       .senderName(mail.getEmployee().getName())
			       .senderEmail(mail.getEmployee().getEmail())
			       .to(safeTo)
			       .cc(safeCc)
			       .bcc(safeBcc)
			       .boxId(mailbox.getBoxId())
			       .mailboxType(mailbox.getTypeId().getValue1())
			       .isRead(mailbox.getIsRead())
			       .deletedStatus(mailbox.getDeletedStatus())
			       .attachments(safeFiles)
			       .toCount(toList.size())
			       .ccCount(ccList.size())
			       .bccCount(bccList.size())
			       .attachmentCount(attachmentFiles.size())
			       .build();
	}
}
