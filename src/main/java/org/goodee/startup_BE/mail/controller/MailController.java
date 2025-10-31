package org.goodee.startup_BE.mail.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.mail.dto.MailDetailResponseDTO;
import org.goodee.startup_BE.mail.dto.MailSendRequestDTO;
import org.goodee.startup_BE.mail.dto.MailSendResponseDTO;
import org.goodee.startup_BE.mail.dto.MailUpdateRequestDTO;
import org.goodee.startup_BE.mail.service.MailService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mails")
public class MailController {
	private final MailService mailService;
	
	// 메일 작성
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<APIResponseDTO<MailSendResponseDTO>> sendMail(@Valid @ModelAttribute MailSendRequestDTO requestDTO, Authentication auth) {
		String username = auth.getName();

		MailSendResponseDTO responseDTO = mailService.sendMail(requestDTO, username);

		APIResponseDTO<MailSendResponseDTO> response = APIResponseDTO.<MailSendResponseDTO>builder()
			                                               .message("메일 발송 성공")
			                                               .data(responseDTO)
			                                               .build();
		return ResponseEntity.ok(response);
	}
	
	// 메일 수정
	@PutMapping(value = "/{mailId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<APIResponseDTO<MailSendResponseDTO>> updateMail(@PathVariable Long mailId, @Valid @ModelAttribute MailUpdateRequestDTO requestDTO, Authentication auth) {
		String username = auth.getName();
		
		MailSendResponseDTO responseDTO = mailService.updateMail(mailId, requestDTO, username);
		
		APIResponseDTO<MailSendResponseDTO> response = APIResponseDTO.<MailSendResponseDTO>builder()
			                                               .message("수정 메일 발송 성공")
			                                               .data(responseDTO)
			                                               .build();
		return ResponseEntity.ok(response);
	}
	
	// 메일 상세 조회
	@GetMapping("/{mailId}")
	public ResponseEntity<APIResponseDTO<MailDetailResponseDTO>> getMailDetail(
		@PathVariable Long mailId, @RequestParam(required = false, defaultValue = "false") boolean isRead, Authentication auth
	) {
		String username = auth.getName();
		MailDetailResponseDTO responseDTO = mailService.getMailDetail(mailId, username, isRead);
		
		APIResponseDTO<MailDetailResponseDTO> response = APIResponseDTO.<MailDetailResponseDTO>builder()
			                                                 .message("메일 상세 조회")
			                                                 .data(responseDTO)
			                                                 .build();
		return ResponseEntity.ok(response);
	}
}
