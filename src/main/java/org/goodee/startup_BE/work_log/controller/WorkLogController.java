package org.goodee.startup_BE.work_log.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.work_log.dto.WorkLogCodeListDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogRequestDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO;

import org.goodee.startup_BE.work_log.service.WorkLogService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/worklogs")
@Tag(name = "WorkLog", description = "업무일지 API")
public class WorkLogController {
	private final WorkLogService workLogService;
	
	// 업무일지 등록
	@Operation(
		summary = "업무일지 등록",
		description = "로그인한 사용자가 신규 업무일지를 등록합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "등록 성공",
		content = @Content(schema = @Schema(implementation = WorkLogResponseDTO.class))
	)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<APIResponseDTO<WorkLogResponseDTO>> createWorkLog(
					@Valid @ModelAttribute WorkLogRequestDTO workLogDTO,
					@Parameter(hidden = true) Authentication auth
	) {
		WorkLogResponseDTO createWorkLog = workLogService.saveWorkLog(workLogDTO, auth.getName());

		APIResponseDTO<WorkLogResponseDTO> response = APIResponseDTO.<WorkLogResponseDTO>builder()
			                                   .message("업무일지 등록 성공")
			                                   .data(createWorkLog)
			                                   .build();

		return ResponseEntity.ok(response);
	}
	
	// 업무일지 수정
	@Operation(
		summary = "업무일지 수정",
		description = "해당 ID의 업무일지를 수정합니다. 작성자만 수정 가능."
	)
	@ApiResponse(
		responseCode = "200",
		description = "수정 성공",
		content = @Content(schema = @Schema(implementation = WorkLogResponseDTO.class))
	)
	@PutMapping("/{id}")
	public ResponseEntity<APIResponseDTO<WorkLogResponseDTO>> modifyWorkLog(
					@Valid @RequestBody WorkLogRequestDTO workLogDTO,
					@Parameter(description = "업무일지 ID", example = "1") @PathVariable(value="id") Long workLogId,
					@Parameter(hidden = true) Authentication auth
	) {
		workLogDTO.setWorkLogId(workLogId);
		WorkLogResponseDTO modifyWorkLog = workLogService.updateWorkLog(workLogDTO, auth.getName());  // common.exception 패키지에 exceptionHandler 추가
		
		APIResponseDTO<WorkLogResponseDTO> response = APIResponseDTO.<WorkLogResponseDTO>builder()
			                                              .message("업무일지 수정 성공")
			                                              .data(modifyWorkLog)
			                                              .build();
		
		return ResponseEntity.ok(response);
	}
	
	// 업무일지 삭제
	@Operation(
		summary = "업무일지 삭제",
		description = "해당 ID의 업무일지를 소프트 삭제합니다. 작성자만 삭제 가능."
	)
	@ApiResponse(responseCode = "200", description = "삭제 성공")
	@DeleteMapping
	public ResponseEntity<APIResponseDTO<Void>> deleteWorkLog(
		@Parameter(description = "업무일지 ID", example = "1") @RequestBody List<Long> ids,
		@Parameter(hidden = true) Authentication auth) {
		workLogService.deleteWorkLog(ids, auth.getName());  // common.exception 패키지에 exceptionHandler 추가
		APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
			                                .message("업무일지 삭제 성공")
			                                .build();
		return ResponseEntity.ok(response);
	}
	
	// 업무일지 조회 (상세보기)
	@Operation(
		summary = "업무일지 상세 조회",
		description = "해당 ID의 업무일지를 상세 조회합니다. 조회 시 읽음 처리됩니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "조회 성공",
		content = @Content(schema = @Schema(implementation = WorkLogResponseDTO.class))
	)
	@GetMapping("/{id}")
	public ResponseEntity<APIResponseDTO<WorkLogResponseDTO>> getWorkLog(
					@Parameter(description = "업무일지 ID", example = "1") @PathVariable(value="id") Long workLogId,
					@Parameter(hidden = true) Authentication auth) {
		WorkLogResponseDTO dto = workLogService.getWorkLogDetail(workLogId, auth.getName());
		APIResponseDTO<WorkLogResponseDTO> response = APIResponseDTO.<WorkLogResponseDTO>builder()
			                                              .message("업무일지 조회 성공")
			                                              .data(dto)
			                                              .build();
		return ResponseEntity.ok(response);
	}
	
	// 업무일지 조회 (리스트 - 전체/부서/나의)
	@Operation(
		summary = "업무일지 목록 조회",
		description = "나의/전체/부서별 업무일지를 페이지네이션으로 조회합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "목록 조회 성공",
		content = @Content(schema = @Schema(implementation = WorkLogResponseDTO.class))
	)
	@GetMapping
	public ResponseEntity<APIResponseDTO<Page<WorkLogResponseDTO>>> list(
		@Parameter(description = "조회 유형 (my|all|dept)", example = "my")
		@RequestParam(defaultValue = "all") String type,
		@Parameter(description = "페이지 번호(0부터 시작)", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기", example = "10")
		@RequestParam(defaultValue = "10") int size,
		@Parameter(hidden = true) Authentication auth
	) {
		var data = workLogService.getWorkLogList(auth.getName(), type, page, size);
		var body = APIResponseDTO.<Page<WorkLogResponseDTO>>builder()
			           .message("업무일지 목록 조회 성공")
			           .data(data)
			           .build();
		return ResponseEntity.ok(body);
	}
	
	@GetMapping("/codes")
	public ResponseEntity<APIResponseDTO<WorkLogCodeListDTO>> getWorkLogCodes() {
		WorkLogCodeListDTO dto = workLogService.getWorkLogCodes();
		
		APIResponseDTO<WorkLogCodeListDTO> response =
			APIResponseDTO.<WorkLogCodeListDTO>builder()
				.message("업무일지 코드 목록 조회 성공")
				.data(dto)
				.build();
		
		return ResponseEntity.ok(response);
	}
}
