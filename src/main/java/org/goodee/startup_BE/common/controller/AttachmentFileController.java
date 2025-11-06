package org.goodee.startup_BE.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.service.AttachmentFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attachmentFiles")
public class AttachmentFileController {
	private final AttachmentFileService attachmentFileService;
	
	@Operation(
		summary = "파일 다운로드",
		description = "파일 ID로 첨부파일을 다운로드합니다. 응답 헤더(Content-Disposition)에 원본 파일명이 포함됩니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "다운로드 성공",
		content = @Content(
			mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
			schema = @Schema(type = "string", format = "binary")
		)
	)
	@ApiResponse(responseCode = "404", description = "파일이 존재하지 않음")
	@GetMapping("/download/{fileId}")
	public ResponseEntity<Resource> downloadFile(
		@Parameter(description = "파일 ID", example = "123", required = true)
		@PathVariable Long fileId
	) {
			return attachmentFileService.downloadFile(fileId);
	}
}
