package org.goodee.startup_BE.mail.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "MailSendRequestDTO", description = "메일 작성 요청 DTO (multipart/form-data)")
public class MailSendRequestDTO {
	@NotBlank
	@Schema(description = "제목", example = "회의 자료 공유드립니다")
	private String title;       // 메일 제목
	
	@Schema(description = "내용(HTML 가능)", example = "<p>안녕하세요.</p><p>자료 공유드립니다.</p>")
	private String content;     // 메일 내용
	
	@NotEmpty
	@Schema(description = "수신자 이메일 목록")
	private List<String> to;    // 수신자 리스트
	
	@Schema(description = "참조자 이메일 목록")
	private List<String> cc;    // 참조 리스트
	
	@Schema(description = "숨은참조자 이메일 목록")
	private List<String> bcc;   // 숨은 참조 리스트

	
	public Mail toEntity(Employee employee, LocalDateTime sendAt) {
		return Mail.createBasicMail(employee, this.title, this.content, sendAt);
	}
}
