package org.goodee.startup_BE.mail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.mail.dto.*;
import org.goodee.startup_BE.mail.service.MailService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mails")
public class MailController {
	private final MailService mailService;
	
	// 메일 작성
	@Operation(
		summary = "메일 작성",
		description = "로그인한 사용자가 내부메일을 작성/발송합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "발송 성공",
		content = @Content(schema = @Schema(implementation = MailSendResponseDTO.class))
	)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<APIResponseDTO<MailSendResponseDTO>> sendMail(@Valid @ModelAttribute MailSendRequestDTO requestDTO, Authentication auth, @RequestParam(value = "files", required = false) List<MultipartFile> multipartFile) {
		String username = auth.getName();

		MailSendResponseDTO responseDTO = mailService.sendMail(requestDTO, username, multipartFile);

		APIResponseDTO<MailSendResponseDTO> response = APIResponseDTO.<MailSendResponseDTO>builder()
			                                               .message("메일 발송 성공")
			                                               .data(responseDTO)
			                                               .build();
		return ResponseEntity.ok(response);
	}
	
	// 메일 상세 조회
	@Operation(
		summary = "메일 상세 조회",
		description = "메일 상세를 조회합니다. isRead=true로 호출 시 읽음 처리합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "조회 성공",
		content = @Content(schema = @Schema(implementation = MailDetailResponseDTO.class))
	)
	@GetMapping("/{mailId}")
	public ResponseEntity<APIResponseDTO<MailDetailResponseDTO>> getMailDetail(
		@PathVariable Long mailId,
		@RequestParam(required = false) Long boxId,
		@RequestParam(required = false, defaultValue = "false") boolean isRead,
		Authentication auth
	) {
		String username = auth.getName();
		MailDetailResponseDTO responseDTO = mailService.getMailDetail(mailId, boxId, username, isRead);
		
		APIResponseDTO<MailDetailResponseDTO> response = APIResponseDTO.<MailDetailResponseDTO>builder()
			                                                 .message("메일 상세 조회")
			                                                 .data(responseDTO)
			                                                 .build();
		return ResponseEntity.ok(response);
	}
	
	// 메일함 이동 (개인보관함, 휴지통)
	@Operation(
		summary = "메일함 이동(개인보관함/휴지통)",
		description = "선택한 메일함 항목을 지정한 타입으로 이동합니다."
	)
	@ApiResponse(responseCode = "200", description = "이동 성공")
	@PostMapping("/move")
	public ResponseEntity<APIResponseDTO<Void>> moveMail(
		@RequestBody @Validated MailMoveRequestDTO requestDTO, Authentication auth
	) {
		String username = auth.getName();
		
		mailService.moveMails(requestDTO, username);
		
		APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
			                                .message("메일함 이동 성공")
			                                .build();
		
		return ResponseEntity.ok(response) ;
	}
	
	// 메일 삭제
	@Operation(
		summary = "메일 삭제",
		description = "선택한 메일함 항목을 삭제(소프트 삭제 또는 상태 변경)합니다."
	)
	@ApiResponse(responseCode = "200", description = "삭제 성공")
	@DeleteMapping("/delete")
	public ResponseEntity<APIResponseDTO<Void>> deleteMail(
		@RequestBody @Validated MailMoveRequestDTO requestDTO, Authentication auth
	) {
		String username = auth.getName();
		
		mailService.deleteMails(requestDTO, username);
		
		APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
			                                .message("메일 삭제 성공")
			                                .build();
		
		return ResponseEntity.ok(response);
	}
	
	// 메일함 리스트 조회
	@Operation(
		summary = "메일함 리스트 조회",
		description = "INBOX/SENT/MYBOX/TRASH 등 메일함 목록을 페이지네이션으로 조회합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "목록 조회 성공"
	)
	@GetMapping
	public ResponseEntity<APIResponseDTO<Page<MailboxListDTO>>> getMailboxList(
		@RequestParam String type,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		Authentication auth
	) {
		String username = auth.getName();
		
		Page<MailboxListDTO> mailboxList = mailService.getMailboxList(username, type, page, size);
		
		APIResponseDTO<Page<MailboxListDTO>> response = APIResponseDTO.<Page<MailboxListDTO>>builder()
			                                                .message("메일함 리스트 조회 성공")
			                                                .data(mailboxList)
			                                                .build();
		
		return ResponseEntity.ok(response);
	}
}
