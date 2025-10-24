package org.goodee.startup_BE.work_log.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_work_type")
@Getter
public class WorkType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_type_id", nullable = false)
    private Long workTypeId;

    @Column(name = "type_code", nullable = false, length = 50, unique = true)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

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

    protected WorkType() {}

    public static WorkType createWorkType(String typeCode, String typeName, String description, Long sortOrder) {
        WorkType workType = new WorkType();
        workType.typeCode = typeCode;
        workType.typeName = typeName;
        workType.description = description;
        workType.sortOrder = sortOrder;
        return workType;
    }

    public void update(String typeName, String description, Long sortOrder) {
        this.typeName = typeName;
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
