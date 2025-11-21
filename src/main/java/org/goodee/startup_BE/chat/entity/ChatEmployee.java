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
@Table(name = "tbl_chat_employee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("채팅방 참여자 고유 ID")
    private Long chatEmployeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Comment("참여 직원")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("채팅방")
    private ChatRoom chatRoom;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("개인별 채팅방 제목")
    private String displayName;

    // 채팅방의 메세지들이 참여시간 이후인 것만 보여짐.(초대 되기 전 메세지를 못읽게 하기 위해)
    @Column(nullable = false)
    @Comment("참여 시각")
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    @Comment("나가기 여부")
    private Boolean isLeft = false;

    @Column(nullable = false)
    @Comment("채팅방 알림 허용 여부")
    private Boolean isNotify = true;

    // 최초 참여시 '초대되었습니다.' 문구를 마지막으로 읽은 메세지로 등록
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("마지막으로 읽은 메시지 ID")
    private ChatMessage lastReadMessage;

    public static ChatEmployee createChatEmployee(
            Employee employee,
            ChatRoom chatRoom,
            String displayName,
            ChatMessage lastReadMessage) {

        ChatEmployee chatEmployee = new ChatEmployee();
        chatEmployee.employee = employee;
        chatEmployee.chatRoom = chatRoom;
        chatEmployee.displayName = displayName;
        chatEmployee.lastReadMessage = lastReadMessage;

        return chatEmployee;
    }

    @PrePersist
    protected void onPrePersist() {
        joinedAt = LocalDateTime.now();
    }

    // 채팅방 이름 변경 (단일 적용)
    public void changedDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // 채팅방 알림 차단
    public void disableNotify() {
        this.isNotify = false;
    }

    // 채팅방 나가기
    public void leftChatRoom() {
        this.isLeft = true;
    }

    // 채팅방 다시 참여
    public void rejoinChatRoom() {
        this.isLeft = false;
        this.joinedAt = LocalDateTime.now();
    }

    // 마지막 읽은 메시지 업데이트
    public void updateLastReadMessage(ChatMessage lastReadMessage) {
        this.lastReadMessage = lastReadMessage;
    }
}
