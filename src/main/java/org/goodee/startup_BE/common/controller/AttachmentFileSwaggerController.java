//package org.goodee.startup_BE.common.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.enums.ParameterIn;
//import io.swagger.v3.oas.annotations.media.ArraySchema;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.ExampleObject;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.constraints.Positive;
//import lombok.RequiredArgsConstructor;
//import org.goodee.startup_BE.common.dto.AttachmentFileRequestDTO;
//import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
//import org.goodee.startup_BE.common.service.AttachmentFileService;
//import org.goodee.startup_BE.common.validation.ValidationGroups;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.core.io.Resource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartException;
//
//import java.io.IOException;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.*;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/test/attachments")
//@RequiredArgsConstructor
//@Validated
//@Tag(name = "Attachment (Swagger Test)", description = "공용 첨부파일 API 테스트 전용")
//@SecurityRequirement(name = "bearerAuth") // 사용 중인 보안 스키마명이 다르면 수정
//public class AttachmentFileSwaggerController {
//
//	private final AttachmentFileService attachmentFileService;
//
//	@Value("${file.storage.root}")
//	private String storageRoot;
//
//	@Operation(
//		summary = "파일 업로드(테스트)",
//		description = "multipart/form-data 로 files 전송. ownerTypeId/ownerId는 쿼리스트링으로 전달",
//		parameters = {
//			@Parameter(
//				name = "ownerTypeId", in = ParameterIn.QUERY, required = true, example = "101",
//				description = "공통코드의 OwnerType ID"),
//			@Parameter(
//				name = "ownerId", in = ParameterIn.QUERY, required = true, example = "999",
//				description = "소유 엔티티의 PK(예: workLogId)")
//		}
//	)
//	@ApiResponses({
//		@ApiResponse(responseCode = "200", description = "업로드 성공",
//			content = @Content(array = @ArraySchema(schema = @Schema(implementation = AttachmentFileResponseDTO.class)))),
//		@ApiResponse(responseCode = "400", description = "요청 유효성 실패",
//			content = @Content(mediaType = "application/json")),
//		@ApiResponse(responseCode = "404", description = "ownerType 없음",
//			content = @Content(mediaType = "application/json")),
//		@ApiResponse(responseCode = "500", description = "서버 오류",
//			content = @Content(mediaType = "application/json"))
//	})
//	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<List<AttachmentFileResponseDTO>> upload(
//		@RequestParam @Positive Long ownerTypeId,
//		@RequestParam @Positive Long ownerId,
//		@Validated(ValidationGroups.Attachment.Upload.class) @ModelAttribute AttachmentFileRequestDTO dto
//	) {
//		return ResponseEntity.ok(attachmentFileService.uploadFiles(dto, ownerTypeId, ownerId));
//	}
//
//	@Operation(
//		summary = "파일 목록 조회(테스트)",
//		parameters = {
//			@Parameter(name = "ownerTypeId", in = ParameterIn.QUERY, required = true, example = "101",
//				description = "공통코드의 OwnerType ID"),
//			@Parameter(name = "ownerId", in = ParameterIn.QUERY, required = true, example = "999",
//				description = "소유 엔티티의 PK(예: workLogId)")
//		}
//	)
//	@ApiResponses({
//		@ApiResponse(responseCode = "200", description = "조회 성공",
//			content = @Content(array = @ArraySchema(schema = @Schema(implementation = AttachmentFileResponseDTO.class)))),
//		@ApiResponse(responseCode = "404", description = "ownerType 없음",
//			content = @Content(mediaType = "application/json"))
//	})
//	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<List<AttachmentFileResponseDTO>> list(
//		@RequestParam @Positive Long ownerTypeId,
//		@RequestParam @Positive Long ownerId
//	) {
//		return ResponseEntity.ok(attachmentFileService.listFiles(ownerTypeId, ownerId));
//	}
//
//	@Operation(
//		summary = "파일 메타 조회(테스트)",
//		parameters = {
//			@Parameter(name = "fileId", in = ParameterIn.PATH, required = true, example = "123",
//				description = "파일 ID")
//		}
//	)
//	@ApiResponses({
//		@ApiResponse(responseCode = "200", description = "조회 성공",
//			content = @Content(schema = @Schema(implementation = AttachmentFileResponseDTO.class))),
//		@ApiResponse(responseCode = "404", description = "파일 없음",
//			content = @Content(mediaType = "application/json"))
//	})
//	@GetMapping(value = "/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<AttachmentFileResponseDTO> meta(@PathVariable @Positive Long fileId) {
//		AttachmentFileRequestDTO req = new AttachmentFileRequestDTO(fileId, null);
//		return ResponseEntity.ok(attachmentFileService.resolveFile(req));
//	}
//
//	@Operation(
//		summary = "파일 다운로드(테스트)",
//		description = "물리 파일 스트리밍 응답",
//		parameters = {
//			@Parameter(name = "fileId", in = ParameterIn.PATH, required = true, example = "123",
//				description = "파일 ID")
//		}
//	)
//	@ApiResponses({
//		@ApiResponse(responseCode = "200", description = "다운로드 성공",
//			content = @Content(mediaType = "application/octet-stream",
//				schema = @Schema(type = "string", format = "binary"),
//				examples = @ExampleObject(name = "binary"))),
//		@ApiResponse(responseCode = "404", description = "파일 없음",
//			content = @Content(mediaType = "application/json"))
//	})
//	@GetMapping(value = "/{fileId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
//	public ResponseEntity<Resource> download(@PathVariable @Positive Long fileId) throws IOException {
//		AttachmentFileRequestDTO req = new AttachmentFileRequestDTO(fileId, null);
//		AttachmentFileResponseDTO meta = attachmentFileService.resolveFile(req);
//
//		Path abs = Paths.get(storageRoot).resolve(meta.getStoragePath()).normalize();
//		if (!Files.exists(abs)) return ResponseEntity.notFound().build();
//
//		Resource resource = new FileSystemResource(abs);
//		String encoded = URLEncoder.encode(meta.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
//
//		return ResponseEntity.ok()
//			       .contentType(MediaType.parseMediaType(meta.getMimeType()))
//			       .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
//			       .contentLength(Files.size(abs))
//			       .body(resource);
//	}
//
//	@Operation(
//		summary = "파일 삭제(논리삭제, 테스트)",
//		parameters = {
//			@Parameter(name = "fileId", in = ParameterIn.PATH, required = true, example = "123",
//				description = "파일 ID")
//		}
//	)
//	@ApiResponses({
//		@ApiResponse(responseCode = "204", description = "삭제 성공"),
//		@ApiResponse(responseCode = "404", description = "파일 없음",
//			content = @Content(mediaType = "application/json"))
//	})
//	@DeleteMapping("/{fileId}")
//	public ResponseEntity<Void> delete(@PathVariable @Positive Long fileId) {
//		AttachmentFileRequestDTO req = new AttachmentFileRequestDTO(fileId, null);
//		attachmentFileService.deleteFile(req);
//		return ResponseEntity.noContent().build();
//	}
//
//	@ExceptionHandler(MultipartException.class)
//	@ApiResponses({
//		@ApiResponse(responseCode = "400", description = "멀티파트 처리 오류",
//			content = @Content(mediaType = "text/plain",
//				examples = @ExampleObject(value = "멀티파트 처리 오류: ...")))
//	})
//	public ResponseEntity<String> handleMultipart(MultipartException e) {
//		return ResponseEntity.badRequest().body("멀티파트 처리 오류: " + e.getMessage());
//	}
//}