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
@Table(name = "tbl_chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @Column(name = "chat_room_id")
    @Comment("채팅방 고유 ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("사원 고유 ID-FK")
    private Employee employeeId;

    @Comment("채팅방 이름")
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String name;

    @CreationTimestamp
    @Comment("생성일")
    @Column(nullable = false, name = "create_at")
    private LocalDateTime createAt;

    @Comment("삭제 여부")
    @Column(nullable = false, name = "is_deleted")
    private boolean isDeleted;

    public static ChatRoom createChatRoom(Employee employeeId, String name) {
        ChatRoom chatRoom = new ChatRoom();

        chatRoom.employeeId = employeeId;
        chatRoom.name = name;

        return chatRoom;
    }
}
