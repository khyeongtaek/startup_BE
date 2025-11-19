package org.goodee.startup_BE.attendance.entity;


import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.attendance.dto.AttendanceWorkHistoryResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Table(name= "tbl_attendance_history")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
public class AttendanceWorkHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_code_id", referencedColumnName = "common_code_id", nullable = false)
    private CommonCode actionCode;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;


    //  Entity → ResponseDTO 변환 메서드
    public AttendanceWorkHistoryResponseDTO toResponseDTO() {
        return AttendanceWorkHistoryResponseDTO.builder()
                .historyId(this.historyId)
                .employeeName(this.employee != null ? this.employee.getName() : null)
                .actionCode(this.actionCode != null ? this.actionCode.getValue1() : null)
                .actionTime(this.actionTime)
                .build();
    }

}
