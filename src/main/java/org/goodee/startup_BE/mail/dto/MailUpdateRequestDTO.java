package org.goodee.startup_BE.mail.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(exclude = "attachmentFiles")
public class MailUpdateRequestDTO {
	private String title;
	private String content;
	
	// 수신 타입별 리스트
	private List<String> to;
	private List<String> cc;
	private List<String> bcc;
	
	// 업로드할 첨부파일
	private List<MultipartFile> attachmentFiles;
	
	// 삭제할 첨부파일
	private List<Long> deleteAttachmentFileIds;
}
