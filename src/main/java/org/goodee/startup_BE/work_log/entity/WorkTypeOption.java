package org.goodee.startup_BE.work_log.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_work_type_option", uniqueConstraints = {@UniqueConstraint(columnNames = {"work_type_id", "option_code"})})
@Getter
public class WorkTypeOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id", nullable = false)
    private WorkType workType;

    @Column(name = "option_code", nullable = false, length = 50)
    private String optionCode;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    private String description;

    @Column(name = "sort_order", nullable = false)
    private Long sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onPrePersist() {
        if(sortOrder == null) sortOrder = 0L;
        if(isActive == null) isActive = true;
        if(createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }


    protected WorkTypeOption() {}

    public static WorkTypeOption createWorkTypeOption(WorkType workType, String optionCode, String optionName, String description, Long sortOrder) {
        WorkTypeOption workTypeOption = new WorkTypeOption();
        workTypeOption.workType = workType;
        workTypeOption.optionCode = optionCode;
        workTypeOption.optionName = optionName;
        workTypeOption.description = description;
        workTypeOption.sortOrder = sortOrder;
        return workTypeOption;
    }

    public void update(String optionName, String description, Long sortOrder) {
        this.optionName = optionName;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
