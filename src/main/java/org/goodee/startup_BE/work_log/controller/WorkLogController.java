package org.goodee.startup_BE.work_log.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogRequestDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO;

import org.goodee.startup_BE.work_log.service.WorkLogService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/worklogs")
public class WorkLogController {
	private final WorkLogService workLogService;

	// 스웨거 문서 작성 - ai 가 잘 작성해줌
	
	// 업무일지 등록
	@PostMapping
	public ResponseEntity<APIResponseDTO<WorkLogResponseDTO>> createWorkLog(@Valid @RequestBody WorkLogRequestDTO workLogDTO, Authentication auth) {
		WorkLogResponseDTO createWorkLog = workLogService.saveWorkLog(workLogDTO, auth.getName());

		APIResponseDTO<WorkLogResponseDTO> response = APIResponseDTO.<WorkLogResponseDTO>builder()
			                                   .message("업무일지 등록 성공")
			                                   .data(createWorkLog)
			                                   .build();

		return ResponseEntity.ok(response);
	}
	
	// 업무일지 수정
	@PutMapping("/{id}")
	public ResponseEntity<APIResponseDTO<WorkLogResponseDTO>> modifyWorkLog(@Valid @RequestBody WorkLogRequestDTO workLogDTO, @PathVariable(value="id") Long workLogId, Authentication auth) {
		workLogDTO.setWorkLogId(workLogId);
		WorkLogResponseDTO modifyWorkLog = workLogService.updateWorkLog(workLogDTO, auth.getName());  // common.exception 패키지에 exceptionHandler 추가
		
		APIResponseDTO<WorkLogResponseDTO> response = APIResponseDTO.<WorkLogResponseDTO>builder()
			                                              .message("업무일지 수정 성공")
			                                              .data(modifyWorkLog)
			                                              .build();
		
		return ResponseEntity.ok(response);
	}
	
	// 업무일지 삭제
	@DeleteMapping("/{id}")
	public ResponseEntity<APIResponseDTO<Void>> deleteWorkLog(@PathVariable Long id, Authentication auth) {
		workLogService.deleteWorkLog(id, auth.getName());  // common.exception 패키지에 exceptionHandler 추가
		APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
			                                .message("업무일지 삭제 성공")
			                                .build();
		return ResponseEntity.ok(response);
	}
	
	// 업무일지 조회 (상세보기)
	@GetMapping("/{id}")
	public ResponseEntity<APIResponseDTO<WorkLogResponseDTO>> getWorkLog(@PathVariable(value="id") Long workLogId, Authentication auth) {
		WorkLogResponseDTO dto = workLogService.getWorkLogDetail(workLogId, auth.getName());
		APIResponseDTO<WorkLogResponseDTO> response = APIResponseDTO.<WorkLogResponseDTO>builder()
			                                              .message("업무일지 조회 성공")
			                                              .data(dto)
			                                              .build();
		return ResponseEntity.ok(response);
	}
	
	// 업무일지 조회하기
	@GetMapping
	public ResponseEntity<APIResponseDTO<Page<WorkLogResponseDTO>>> list(
		@RequestParam(defaultValue = "my") String type,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		Authentication auth
	) {
		var data = workLogService.getWorkLogList(auth.getName(), type, page, size);
		var body = APIResponseDTO.<Page<WorkLogResponseDTO>>builder()
			           .message("업무일지 목록 조회 성공")
			           .data(data)
			           .build();
		return ResponseEntity.ok(body);
	}
}
