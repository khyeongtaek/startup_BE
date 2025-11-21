package org.goodee.startup_BE.mail.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.mail.entity.Mailbox;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Schema(name = "MailDetailResponseDTO", description = "메일 상세 응답 DTO")
public class MailDetailResponseDTO {
	@Schema(description = "메일 ID", example = "100")
	private Long mailId;
	
	@Schema(description = "제목", example = "회의 자료 공유드립니다")
	private String title;
	
	@Schema(description = "내용(HTML 가능)")
	private String content;
	
	@Schema(description = "EML 파일 경로(내부 저장 경로)", example = "/data/eml/2025/11/03/abcd1234.eml")
	private String emlPath;
	
	// 발신자 정보
	@Schema(description = "발송 시각(또는 작성 시각)", example = "2025-11-03T10:05:00")
	private LocalDateTime sendAt;
	@Schema(description = "발신자 직원 ID", example = "7")
	private Long senderId;
	@Schema(description = "보낸 사람", example = "홍길동 <sender@example.com>")
	private String senderName;
	@Schema(description = "발신자 이메일", example = "sender@example.com")
	private String senderEmail;
	private String senderProfileImg;
	private String senderPosition;
	private String senderDepartment;
	
	// 수신자 정보
	@ArraySchema(schema = @Schema(description = "수신자 이메일", example = "user1@example.com"))
	private List<MailReceiverDTO> to;
	@ArraySchema(schema = @Schema(description = "참조자 이메일", example = "user2@example.com"))
	private List<MailReceiverDTO> cc;
	@ArraySchema(schema = @Schema(description = "숨은참조자 이메일", example = "user3@example.com"))
	private List<MailReceiverDTO> bcc;
	
	// 메일함 정보
	@Schema(description = "메일함 항목 ID(boxId)", example = "55")
	private Long boxId;
	@Schema(description = "메일함 타입", example = "INBOX") // INBOX, SENT, MYBOX, TRASH 등
	private String mailboxType;
	@Schema(description = "읽음 여부", example = "true")
	private Boolean isRead;
	@Schema(description = "삭제 상태 플래그", example = "0")
	private Byte deletedStatus;
	
	// 첨부
	@ArraySchema(schema = @Schema(implementation = AttachmentFileResponseDTO.class))
	@Schema(description = "첨부파일 목록")
	private List<AttachmentFileResponseDTO> attachments;
	
	// 카운팅을 위한 필드
	@Schema(description = "수신자 수", example = "2")
	private int toCount;
	@Schema(description = "참조자 수", example = "1")
	private int ccCount;
	@Schema(description = "숨은참조자 수", example = "0")
	private int bccCount;
	@Schema(description = "첨부파일 수", example = "3")
	private int attachmentCount;
	
	public static MailDetailResponseDTO toDTO(
		Mail mail, List<MailReceiverDTO> toList, List<MailReceiverDTO> ccList, List<MailReceiverDTO> bccList,
		Mailbox mailbox, List<AttachmentFileResponseDTO> attachmentFiles
	) {
		List<MailReceiverDTO> safeTo  = (toList  == null) ? Collections.emptyList() : toList;
		List<MailReceiverDTO> safeCc  = (ccList  == null) ? Collections.emptyList() : ccList;
		List<MailReceiverDTO> safeBcc = (bccList == null) ? Collections.emptyList() : bccList;
		List<AttachmentFileResponseDTO> safeFiles = (attachmentFiles == null) ? Collections.emptyList() : attachmentFiles;
		
		Employee sender = mail.getEmployee();
		Long senderId = (sender == null) ? null : sender.getEmployeeId();
		String senderName = (sender == null) ? "정보 없음" : sender.getName();
		String senderEmail = (sender == null) ? null : sender.getEmail();
		String senderProfileImg = (sender == null) ? null : sender.getProfileImg();
		String senderPosition = (sender == null) ? null : sender.getPosition().getValue1();
		String senderDepartment = (sender == null) ? null : sender.getDepartment().getValue1();
		
		String mailboxType =
			mailbox.getDeletedStatus() != null && mailbox.getDeletedStatus() == 1
				? "TRASH"
				: mailbox.getTypeId().getValue1();
		
		return MailDetailResponseDTO.builder()
			       .mailId(mail.getMailId())
			       .title(mail.getTitle())
			       .content(mail.getContent())
			       .sendAt(mail.getSendAt())
			       .emlPath(mail.getEmlPath())
			       .senderId(senderId)
			       .senderName(senderName)
			       .senderEmail(senderEmail)
			       .senderProfileImg(senderProfileImg)
			       .senderPosition(senderPosition)
			       .senderDepartment(senderDepartment)
			       .to(safeTo)
			       .cc(safeCc)
			       .bcc(safeBcc)
			       .boxId(mailbox.getBoxId())
			       .mailboxType(mailboxType)
			       .isRead(mailbox.getIsRead())
			       .deletedStatus(mailbox.getDeletedStatus())
			       .attachments(safeFiles)
			       .toCount(safeTo.size())
			       .ccCount(safeCc.size())
			       .bccCount(safeBcc.size())
			       .attachmentCount(safeFiles.size())
			       .build();
	}
}
