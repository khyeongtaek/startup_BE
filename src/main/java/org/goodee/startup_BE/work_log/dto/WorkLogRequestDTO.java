package org.goodee.startup_BE.work_log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.work_log.entity.WorkLog;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class WorkLogRequestDTO {    // 등록/수정
	@Schema(description = "업무일지 ID (수정 시에만 사용)", example = "42")
	private Long workLogId;
	
	@NotNull
	@Positive
	@Schema(description = "업무구분 코드 ID (CommonCode PK)", example = "101")
	private Long workTypeId;          // 업무구분 ID
	
	@NotNull
	@Positive
	@Schema(description = "업무옵션 코드 ID (CommonCode PK)", example = "202")
	private Long workOptionId;        // 업무옵션 ID
	
	@NotNull
	@PastOrPresent
	@Schema(description = "업무 수행일(작성일 아님). yyyy-MM-dd 형식", example = "2025-10-28")
	private LocalDateTime workDate;   // 작성일
	
	@NotBlank
	@Size(max = 100)
	@Schema(description = "업무일지 제목", example = "고객사 미팅 및 요구사항 정리")
	private String title;             // 제목
	
	@Size(max = 1000)
	@Schema(description = "업무 상세 내용", example = "고객 A사와 10:00 미팅 진행. 개선 요구 3건 수집.")
	private String content;           // 내용
	
	// DTO -> Entity
	public WorkLog toEntity(Employee employee, CommonCode workType, CommonCode workOption) {
		return WorkLog.createWorkLog(
			employee, workType, workOption, this.workDate, this.title, this.content);
	}
}
