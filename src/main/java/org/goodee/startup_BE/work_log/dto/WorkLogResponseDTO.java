package org.goodee.startup_BE.work_log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.work_log.entity.WorkLog;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class WorkLogResponseDTO {
	@Schema(description = "업무일지 PK", example = "42")
	private Long workLogId;         // 업무일지 고유 ID
	
	@Schema(description = "작성자 이름", example = "홍길동")
	private String employeeName;    // 직원 이름
	private String employeeProfileImg;
	private String employeeDepartment;
	
	@Schema(description = "업무구분 이름", example = "프로젝트")
	private String workTypeName;    // 업무구분 이름 (한글)
	
	@Schema(description = "업무세부옵션 이름", example = "고객 미팅")
	private String workOptionName;  // 업무옵션 이름 (한글)
	
	@Schema(description = "업무 수행일", example = "2025-10-28")
	private LocalDateTime workDate; // 작업일
	
	@Schema(description = "제목", example = "A사 요구사항 정리")
	private String title;           // 제목
	
	@Schema(description = "내용", example = "미팅 결과 요약 및 액션 아이템 정리")
	private String content;         // 내용
	
	private Long workTypeId;
	private Long workOptionId;
	
	@Builder.Default
	@Schema(description = "읽음 여부 (리스트에서 true면 이미 열람함)", example = "false")
	private Boolean isRead = false; // 읽음 여부
	
	public static WorkLogResponseDTO toDTO(WorkLog workLog) {
		if(workLog == null) return null;
		
		Employee writer = workLog.getEmployee();
		String writerName = (writer == null) ? "정보 없음" : writer.getName();;
		String writerProfileImg = (writer == null) ? null : writer.getProfileImg();
		String writerDepartment = (writer == null) ? null : writer.getDepartment().getValue1();
		
		return WorkLogResponseDTO.builder()
			       .workLogId(workLog.getWorkLogId())
			       .employeeName(writerName)
			       .employeeProfileImg(writerProfileImg)
			       .employeeDepartment(writerDepartment)
			       .workTypeName(workLog.getWorkType().getValue2())
			       .workOptionName(workLog.getWorkOption().getValue2())
			       .workTypeId(workLog.getWorkType().getCommonCodeId())
			       .workOptionId(workLog.getWorkOption().getCommonCodeId())
			       .workDate(workLog.getWorkDate())
			       .title(workLog.getTitle())
			       .content(workLog.getContent())
			       .build();
	}
}
