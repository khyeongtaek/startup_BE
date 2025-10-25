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

    @Comment("팀 채팅방 여부")
    @Column(nullable = false)
    private boolean isTeam;

    @CreationTimestamp
    @Comment("생성일")
    @Column(nullable = false, name = "create_at")
    private LocalDateTime createAt;


    public static ChatRoom createChatRoom(Employee employee, String name,boolean isTeam, LocalDateTime createAt) {
        ChatRoom chatRoom = new ChatRoom();

        chatRoom.employee = employee;
        chatRoom.name = name;
        chatRoom.isTeam = isTeam;
        chatRoom.createAt = createAt;

        return chatRoom;
    }
}
