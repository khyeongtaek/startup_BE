package org.goodee.startup_BE.mail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.mail.entity.Mail;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(exclude = "attachmentFiles")
public class MailSendRequestDTO {
	@NotBlank
	private String title;       // 메일 제목
	private String content;     // 메일 내용
	
	@NotEmpty
	private List<String> to;    // 수신자 리스트
	private List<String> cc;    // 참조 리스트
	private List<String> bcc;   // 숨은 참조 리스트
	private List<MultipartFile> attachmentFiles;    // 첨부파일
	
	public Mail toEntity(Employee employee, LocalDateTime sendAt) {
		return Mail.createBasicMail(employee, this.title, this.content, sendAt);
	}
}
