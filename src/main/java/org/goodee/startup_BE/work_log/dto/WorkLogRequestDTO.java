package org.goodee.startup_BE.work_log.dto;

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
	private Long workLogId;
	
	@NotNull
	@Positive
	private Long workTypeId;          // 업무구분 ID
	
	@NotNull
	@Positive
	private Long workOptionId;        // 업무옵션 ID
	
	@NotNull
	@PastOrPresent
	private LocalDateTime workDate;   // 작성일
	
	@NotBlank
	@Size(max = 100)
	private String title;             // 제목
	
	@Size(max = 1000)
	private String content;           // 내용
	
	// DTO -> Entity
	public WorkLog toEntity(Employee employee, CommonCode workType, CommonCode workOption) {
		return WorkLog.createWorkLog(
			employee, workType, workOption, this.workDate, this.title, this.content);
	}
}
