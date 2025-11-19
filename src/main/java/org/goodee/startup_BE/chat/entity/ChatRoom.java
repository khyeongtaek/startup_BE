package org.goodee.startup_BE.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @Comment("채팅방 고유 ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("채팅방 생성자")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @Comment("채팅방 이름")
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String name;

    @Comment("팀 채팅방 여부")
    @Column(nullable = false)
    private Boolean isTeam;

    @Comment("생성일")
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Comment("삭제 여부")
    @Column(nullable = false)
    private Boolean isDeleted = false;


    public static ChatRoom createChatRoom(Employee employee, String name,Boolean isTeam) {
        ChatRoom chatRoom = new ChatRoom();

        chatRoom.employee = employee;
        chatRoom.name = name;
        chatRoom.isTeam = isTeam;

        return chatRoom;
    }

    @PrePersist
    protected void onPrePersist() {
        createdAt = LocalDateTime.now();
    }

    // 채팅방 삭제 여부
    public void deleteRoom() { this.isDeleted = true; }

    // 1:1 채팅방을 팀 채팅방으로 변경 한다.
    public void updateToTeamRoom() {
        if(!this.isTeam) {
            this.isTeam = true;
        }
    }
}
