package org.goodee.startup_BE.attendance.entity;


import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDateTime;

@Table(name="tbl_approval_template")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_name", columnDefinition = "LONGTEXT")
    private String templateName;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="created_by" , referencedColumnName = "employee_id")
    private Employee createdBy;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean is_deleted;
}

