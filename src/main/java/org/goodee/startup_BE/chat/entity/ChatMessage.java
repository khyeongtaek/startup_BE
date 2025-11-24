package org.goodee.startup_BE.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("채팅 메시지 고유 ID")
    private Long chatMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("채팅방 ID")
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("메시지 타입 (CHAT_USER, CHAT_SYSTEM)")
    private OwnerType messageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    @Comment("직원 ID")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @Comment("채팅 내용")
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Comment("생성 시각")
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Comment("삭제 여부")
    @Column(nullable = false)
    private Boolean isDeleted = false;

    public static ChatMessage createChatMessage(
            ChatRoom chatRoom,
            Employee employee,
            String content,
            OwnerType MessageType
    ) {
        ChatMessage chatMessage = new ChatMessage();

        chatMessage.chatRoom = chatRoom;
        chatMessage.employee = employee;
        chatMessage.content = content;
        chatMessage.messageType = MessageType;

        return chatMessage;
    }

    public static ChatMessage createChatMessage(
            ChatRoom chatRoom,
            Employee employee,
            String content
    ) {
        if (employee == null) {
            throw new IllegalArgumentException("사용자 메시지에는 사용자 정보가 필수 입니다");
        }
        return createChatMessage(chatRoom, employee, content, OwnerType.CHAT_USER);
    }

    public static ChatMessage createSystemMessage(
            ChatRoom chatRoom,
            String content
    ) {
        return createChatMessage(chatRoom, null, content, OwnerType.CHAT_SYSTEM);
    }

    @PrePersist
    protected void onPrePersist() {
        createdAt = LocalDateTime.now();
    }

    // 채팅 메시지 삭제 (소프트 삭제)
    public void deleteChatMessage() {
        this.isDeleted = true;
    }
}
