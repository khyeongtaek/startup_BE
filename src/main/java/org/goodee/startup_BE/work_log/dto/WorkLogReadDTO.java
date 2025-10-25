package org.goodee.startup_BE.work_log.dto;

import lombok.*;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.goodee.startup_BE.work_log.entity.WorkLogRead;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class WorkLogReadDTO {
    private Long workLogId;     // 업무일지 ID
    private Long employeeId;    // 직원 ID
    private Boolean isRead;     // 읽음 여부

    public WorkLogRead toEntity(WorkLog workLogId, Employee employeeId) {
        return WorkLogRead.createWorkLogRead(workLogId, employeeId);
    }

    public static WorkLogReadDTO fromEntity(WorkLogRead workLogRead) {
        return WorkLogReadDTO.builder()
                .workLogId(workLogRead.getWorkLog().getWorkLogId())
                .employeeId(workLogRead.getEmployee().getEmployeeId())
                .isRead(true)       // 엔티티가 존재하면 읽음 처리
                .build();
    }
}
