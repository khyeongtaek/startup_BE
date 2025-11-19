package org.goodee.startup_BE.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_notification")

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("알림 고유 ID")
    private Long notificationId;

    @Comment("수신자 ID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @Comment("알림 출처 (Common Code")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_type", nullable = false)
    private CommonCode ownerType;

    @Comment("알림 링크")
    @Column(columnDefinition = "LONGTEXT")
    private String url;

    @Comment("알림 제목")
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String title;

    @Comment("알림 내용")
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Comment("생성 시각")
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Comment("읽은 시각")
    @Column(nullable = true)
    private LocalDateTime readAt;

    @Comment("삭제 여부")
    @Column(nullable = false)
    private Boolean isDeleted = false;

    public static Notification createNotification(
            Employee employee,
            CommonCode ownerType,
            String url,
            String title,
            String content) {

        Notification n = new Notification();

        n.employee = employee;
        n.ownerType = ownerType;
        n.url = url;
        n.title = title;
        n.content = content;

        return n;
    }

    @PrePersist
    protected void onPrePersist() {
        createdAt = LocalDateTime.now();
    }

    // 알림을 읽었을 때 readAt update
    public void readNotification() {
        if( readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    // 알림 삭제 (소프트 삭제)
    public void deleteNotification() {
        this.isDeleted = true;
    }
}
