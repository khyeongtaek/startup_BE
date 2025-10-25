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
@Table(name="tbl_chat_message")
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
    private ChatRoom chatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("직원 ID")
    private Employee employee;

    @Comment("채팅 내용")
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Comment("생성 시각")
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Comment("삭제 여부")
    @Column(nullable = false)
    private boolean isDeleted = false;

    @Comment("첨부파일 여부")
    @Column(nullable = false)
    private boolean isHasAttachment = false;

    public static ChatMessage createChatMessage(
            ChatRoom chatRoomId,
            Employee employee,
            String content,
            LocalDateTime createdAt,
            boolean isDeleted,
            boolean isHasAttachment) {
        ChatMessage chatMessage = new ChatMessage();

        chatMessage.chatRoomId = chatRoomId;
        chatMessage.employee = employee;
        chatMessage.content = content;
        chatMessage.createdAt = createdAt;
        chatMessage.isDeleted = isDeleted;
        chatMessage.isHasAttachment = isHasAttachment;

        return chatMessage;
    }

    // 채팅 메시지 삭제 (소프트 삭제)
    public void deleteChatMessage() {
        this.isDeleted = true;
    }

    // 첨부 파일 여부
    public void updateHasAttachment() {
        this.isHasAttachment = true;
    }
}
