package org.goodee.startup_BE.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.schedule.service.HolidayService;

import java.util.HashMap;
import java.util.List;

@Tag(name = "Holiday API", description = "공휴일 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(
            summary = "공휴일 정보 조회",
            description = "해당 연도(year), 월(month)에 대한 공휴일 데이터를 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공휴일 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/holidays")
    public ResponseEntity<APIResponseDTO<List<HashMap<String, Object>>>> getHolidays(
            @Parameter(description = "조회할 연도", required = true, example = "2025")
            @RequestParam String year,

            @Parameter(description = "조회할 월", required = true, example = "02")
            @RequestParam String month
    ) {

        List<HashMap<String, Object>> holidayList = holidayService.getHolidays(year, month);

        return ResponseEntity.ok(
                APIResponseDTO.<List<HashMap<String, Object>>>builder()
                        .message("공휴일 조회 성공")
                        .data(holidayList)
                        .build()
        );
    }
}