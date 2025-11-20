package org.goodee.startup_BE.attendance.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import lombok.*;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@Table(name = "tbl_annual_leave")
public class AnnualLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @Column(name = "total_days")
    private Double totalDays;

    @Column(name = "used_days")
    private Double usedDays;


    @Column(name="leave_year")
    private Long year;

    @Column(name= "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted;


    // 팩토리 메서드
    public static AnnualLeave createInitialLeave(Employee employee) {
        return AnnualLeave.builder()
                .employee(employee)
                .totalDays(15.0)
                .usedDays(0.0)
                .year((long) LocalDateTime.now().getYear())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }


    // 잔여 연차 조회
    @Transient
    public Double getRemainingDays() {
        return this.totalDays - this.usedDays;
    }


    // 연차 사용
    public void useDays(Double days) {
        if (days == null || days <= 0) {
            throw new IllegalArgumentException("사용 일수는 1일 이상이어야 합니다.");
        }

        Double remaining = getRemainingDays();

        if (days > remaining) {
            throw new IllegalStateException(
                    String.format("남은 연차가 부족합니다. (요청: %.1f일, 잔여: %.1f일)", days, remaining)
            );
        }

        this.usedDays += days;
        this.updatedAt = LocalDateTime.now();
    }


    // 연차 사용 취소
    public void refundDays(Double days) {
        if (days == null || days <= 0) {
            throw new IllegalArgumentException("환원 일수는 1일 이상이어야 합니다.");
        }

        if (this.usedDays - days < 0) {
            throw new IllegalStateException("환원할 일수가 사용 일수보다 많습니다.");
        }

        this.usedDays -= days;
        this.updatedAt = LocalDateTime.now();
    }


    // 생성 시 연차 자동 세팅
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
        if (this.year == null) {
            this.year = (long) LocalDateTime.now().getYear();
        }
        if (this.totalDays == null) {
            this.totalDays = 15.0;
        }
        if (this.usedDays == null) {
            this.usedDays = 0.0;
        }
    }

    // 수정시 수정 시간 업데이트
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
