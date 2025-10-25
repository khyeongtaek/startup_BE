package org.goodee.startup_BE.work_log.dto;

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
public class WorkLogDTO {
    private Long employeeId;            // Employee 외래키 (작성자)
    private String workType;            // CommonCode 외래키 (업무구분)
    private String workOption;          // CommonCode 외래키 (업무옵션)
    private LocalDateTime workDate;     // 작성일
    private String title;               // 제목
    private String content;             // 본문 내용

    public WorkLog toEntity(
            Employee employeeId,
            CommonCode workType,
            CommonCode workOption
    ) {
        return WorkLog.createWorkLog(
                employeeId,
                workType,
                workOption,
                this.workDate,
                this.title,
                this.content
        );
    }

    public static WorkLogDTO fromEntity(WorkLog workLog) {
        return WorkLogDTO.builder()
                .employeeId(workLog.getEmployee().getEmployeeId())
                .workType(workLog.getWorkType().getValue2())
                .workOption(workLog.getWorkOption().getValue2())
                .workDate(workLog.getWorkDate())
                .title(workLog.getTitle())
                .content(workLog.getContent())
                .build();
    }
}
