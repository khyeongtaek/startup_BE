package org.goodee.startup_BE.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_notification")

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    @Comment("알림 고유 ID")
    private Long notificationId;

    @Comment("수신자 ID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employeeId;

    @Comment("알림 링크")
    @Column(name = "url", columnDefinition = "LONGTEXT")
    private String url;

    @Comment("알림 제목")
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String title;

    @Comment("알림 내용")
    @Column(nullable = false, columnDefinition = "LONGTEXT")
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
    private boolean isDeleted;


    public Notification(String url, String title, String content, LocalDateTime readAt, Boolean isDeleted) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.readAt = readAt;
        this.isDeleted = isDeleted;
    }

    public static Notification createNotification(
            Employee employeeId,
            String url,
            String title,
            String content,
            LocalDateTime createAt,
            LocalDateTime readAt,
            boolean isDeleted) {

        Notification n = new Notification();

        n.employeeId = employeeId;
        n.url = url;
        n.title = title;
        n.content = content;
        n.createAt = LocalDateTime.now();
        n.readAt = readAt;
        n.isDeleted = false;

        return n;
    }

    @Override
    public String toString() {
        return "NotiEntity{" +
                "notificationId=" + notificationId +
                ", employeeId=" + (employeeId != null ? employeeId.getEmployeeId() : null) +
                ", url=" + url +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", createAt=" + createAt +
                ", readAt=" + readAt +
                ", isDeleted=" + isDeleted +
                '}';
    }

    // 알림을 읽었을 때 readAt update
    public void readNotification() {
        this.readAt = LocalDateTime.now();
    }

    // 알림 삭제 (소프트 삭제)
    public void deleteNotification() {
        this.isDeleted = true;
    }
}
