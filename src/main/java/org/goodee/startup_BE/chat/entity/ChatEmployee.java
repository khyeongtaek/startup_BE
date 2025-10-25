package org.goodee.startup_BE.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_chat_employee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("채팅방 참여자 고유 ID")
    private Long chatEmployeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( nullable = false)
    @Comment("참여 직원")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("채팅방")
    private ChatRoom chatRoomId;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("표시 이름")
    private String displayName;

    @Column(nullable = false)
    @Comment("이름 변경 여부")
    private boolean changedDisplayName = false;

    @Column(nullable = false)
    @CreationTimestamp
    @Comment("참여 시각")
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    @Comment("나가기 여부")
    private boolean isLeft = false;

    @Column(nullable = false)
    @Comment("채팅방 알림 차단 여부")
    private boolean isNotify = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("마지막으로 읽은 메시지 ID")
    private ChatMessage lastReadMessageId;

    public static ChatEmployee createChatEmployee(
            Employee employee,
            ChatRoom chatRoomId,
            String displayName,
            boolean changedDisplayName,
            LocalDateTime joinedAt,
            boolean isLeft,
            boolean isNotify,
            ChatMessage lastReadMessageId) {

        ChatEmployee chatEmployee = new ChatEmployee();
        chatEmployee.employee = employee;
        chatEmployee.chatRoomId = chatRoomId;
        chatEmployee.displayName = displayName;
        chatEmployee.changedDisplayName = changedDisplayName;
        chatEmployee.joinedAt = joinedAt;
        chatEmployee.isLeft = isLeft;
        chatEmployee.isNotify = isNotify;
        chatEmployee.lastReadMessageId = lastReadMessageId;

        return chatEmployee;
    }

    // 채팅방 이름 변경 (단일 적용)
    public void chagedDisplayName(String displayName) {
        this.displayName = displayName;
        this.changedDisplayName = true;
    }

    // 채팅방 알림 차단 여부
    public void isNotify() {
        this.isNotify = true;
    }

    // 채팅방 나가기 여부
    public void isLeft() {
        this.isLeft = true;
    }
}
