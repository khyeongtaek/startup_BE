package org.goodee.startup_BE.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_notification")

@NoArgsConstructor
@Getter
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Comment("수신자 ID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", foreignKey = @ForeignKey(name = "fk_notification_employee"))
    private EmployeeEntity employeeId;

    @Comment("관련 리소스 PK")
    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Comment("알림 제목")
    @Column(nullable = false)
    private String title;

    @Comment("알림 내용")
    private String content;

    @Comment("생성 시각")
    @Column(name = "create_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createAt;

    @Comment("읽은 시각")
    @Column(name = "read_at", nullable = true)
    private LocalDateTime readAt;

    @Comment("삭제 여부")
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted ;


    public NotificationEntity(Long refId, String title, String content, LocalDateTime readAt, Boolean isDeleted) {
        this.refId = refId;
        this.title = title;
        this.content = content;
        this.readAt = readAt;
        this.isDeleted = isDeleted;
    }

    public static NotificationEntity create(EmployeeEntity employeeId, Long refId, String title, String content ) {

        if(employeeId == null) throw new IllegalArgumentException("수신자 ID는 필수 입니다");
        if(title == null || title.isBlank()) throw new IllegalArgumentException("알림 제목은 필수 입니다");

        NotificationEntity n = new NotificationEntity();

        n.employeeId = employeeId;
        n.refId = refId;
        n.title = title;
        n.content = content;

        return n;
    }

    @Override
    public String toString() {
        return "NotiEntity{" +
                "notificationId=" + notificationId +
                ", employeeId=" + (employeeId != null ? employeeId.getId() : null) +
                ", refId=" + refId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", createAt=" + createAt +
                ", readAt=" + readAt +
                ", isDeleted=" + isDeleted +
                '}';

    }
}
