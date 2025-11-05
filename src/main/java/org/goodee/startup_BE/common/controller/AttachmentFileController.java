package org.goodee.startup_BE.common.controller;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.service.AttachmentFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attachmentFiles")
public class AttachmentFileController {
	private final AttachmentFileService attachmentFileService;
	
	@GetMapping("/download/{fileId}")
	public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
		return attachmentFileService.downloadFile(fileId);
	}
	
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadFiles(@RequestParam("files") List<MultipartFile> files) {
		attachmentFileService.uploadFiles(files, 68L, 1L); // ✅ 그대로 호출
		return ResponseEntity.ok("업로드 성공");
	}
}
