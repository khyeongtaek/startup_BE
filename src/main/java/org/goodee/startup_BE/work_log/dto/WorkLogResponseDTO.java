package org.goodee.startup_BE.work_log.dto;

import lombok.*;
import org.goodee.startup_BE.work_log.entity.WorkLog;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class WorkLogResponseDTO {
	private Long workLogId;         // 업무일지 고유 ID
	private String employeeName;    // 직원 이름
	private String workTypeName;    // 업무구분 이름 (한글)
	private String workOptionName;  // 업무옵션 이름 (한글)
	private LocalDateTime workDate; // 작업일
	private String title;           // 제목
	private String content;         // 내용
	private Boolean isRead = false; // 읽음 여부
	
	public static WorkLogResponseDTO toDTO(WorkLog workLog) {
		if(workLog == null) return null;
		
		return WorkLogResponseDTO.builder()
			       .workLogId(workLog.getWorkLogId())
			       .employeeName(workLog.getEmployee().getName())
			       .workTypeName(workLog.getWorkType().getValue2())
			       .workOptionName(workLog.getWorkOption().getValue2())
			       .workDate(workLog.getWorkDate())
			       .title(workLog.getTitle())
			       .content(workLog.getContent())
			       .build();
	}
}
