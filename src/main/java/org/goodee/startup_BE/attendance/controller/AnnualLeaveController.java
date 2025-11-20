package org.goodee.startup_BE.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.attendance.dto.AnnualLeaveResponseDTO;
import org.goodee.startup_BE.attendance.entity.AnnualLeave;
import org.goodee.startup_BE.attendance.service.AnnualLeaveService;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Annual Leave API", description = "연차 관리 API")
@RestController
@RequestMapping("/api/annual-leaves")
@RequiredArgsConstructor
public class AnnualLeaveController {

    private final AnnualLeaveService annualLeaveService;

    //  특정 직원의 연차 정보 조회
    @Operation(summary = "연차 조회", description = "직원 ID를 기준으로 연차 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "연차 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 직원의 연차 정보가 존재하지 않음", content = @Content)
    })
    @GetMapping("/{employeeId}")
    public ResponseEntity<APIResponseDTO<AnnualLeaveResponseDTO>> getAnnualLeave(
            @Parameter(description = "조회할 직원 ID", required = true, example = "1")
            @PathVariable Long employeeId
    ) {
        AnnualLeave annualLeave = annualLeaveService.getAnnualLeave(employeeId);
        AnnualLeaveResponseDTO responseDTO = AnnualLeaveResponseDTO.toDTO(annualLeave);

        return ResponseEntity.ok(
                APIResponseDTO.<AnnualLeaveResponseDTO>builder()
                        .message("연차 조회 성공")
                        .data(responseDTO)
                        .build()
        );
    }

    //  연차 사용
    @Operation(summary = "연차 사용", description = "직원이 원하는 일수만큼 연차를 사용합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "연차 사용 완료"),
            @ApiResponse(responseCode = "400", description = "남은 연차 부족 또는 잘못된 요청", content = @Content)
    })
    @PostMapping("/{employeeId}/use")
    public ResponseEntity<APIResponseDTO<AnnualLeaveResponseDTO>> useAnnualLeave(
            @Parameter(description = "연차를 사용할 직원 ID", required = true, example = "1")
            @PathVariable Long employeeId,
            @Parameter(description = "사용할 연차 일수", required = true, example = "2")
            @RequestParam Double days
    ) {
        AnnualLeave updated = annualLeaveService.useAnnualLeave(employeeId, days);
        AnnualLeaveResponseDTO responseDTO = AnnualLeaveResponseDTO.toDTO(updated);

        return ResponseEntity.ok(
                APIResponseDTO.<AnnualLeaveResponseDTO>builder()
                        .message("연차 사용 완료")
                        .data(responseDTO)
                        .build()
        );
    }

    //  연차 사용 취소 (복원)
    @Operation(summary = "연차 복원", description = "직원이 잘못 사용한 연차를 환원합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "연차 복원 완료"),
            @ApiResponse(responseCode = "400", description = "환원할 일수가 잘못되었거나 초과함", content = @Content)
    })
    @PostMapping("/{employeeId}/refund")
    public ResponseEntity<APIResponseDTO<AnnualLeaveResponseDTO>> refundAnnualLeave(
            @Parameter(description = "연차를 복원할 직원 ID", required = true, example = "1")
            @PathVariable Long employeeId,
            @Parameter(description = "복원할 연차 일수", required = true, example = "1")
            @RequestParam Double days
    ) {
        AnnualLeave updated = annualLeaveService.refundAnnualLeave(employeeId, days);
        AnnualLeaveResponseDTO responseDTO = AnnualLeaveResponseDTO.toDTO(updated);

        return ResponseEntity.ok(
                APIResponseDTO.<AnnualLeaveResponseDTO>builder()
                        .message("연차 복원 완료")
                        .data(responseDTO)
                        .build()
        );
    }
}