package org.goodee.startup_BE.mail.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(exclude = "attachmentFiles")
@Schema(name = "MailUpdateRequestDTO", description = "메일 수정 요청 DTO (multipart/form-data)")
public class MailUpdateRequestDTO {
	@Schema(description = "제목", example = "회의 자료(수정본) 공유")
	private String title;
	
	@Schema(description = "내용(HTML 가능)", example = "<p>수정된 자료 공유드립니다.</p>")
	private String content;
	
	// 수신 타입별 리스트
	@Schema(description = "수신자 이메일 목록(전체 대체)")
	private List<String> to;
	@Schema(description = "참조자 이메일 목록(전체 대체)")
	private List<String> cc;
	@Schema(description = "숨은참조자 이메일 목록(전체 대체)")
	private List<String> bcc;
	
	// 삭제할 첨부파일
	@Schema(description = "제거할 첨부파일 ID 목록")
	private List<Long> deleteAttachmentFileIds;
}
