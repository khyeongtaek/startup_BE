package org.goodee.startup_BE.chat.dto;

import lombok.*;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;

import java.time.LocalDateTime;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatRoomListResponseDTO {

    // 방 ID
    private Long chatRoomId;
    // 1:1 상대방 이름 또는 팀방 이름
    private String name;
    // 상대방 프로필
    private String profile;
    // 안 읽은 메시지 수
    private Long unreadCount;
    // 마지막 메시지 시각 또는 내용
    private String lastMessage;
    // 채팅방 목록 정렬을 위한 마지막 메시지
    private LocalDateTime lastMessageCreatedAt;
    // 팀채팅방 여부
    private Boolean isTeam;
    // 사용자 수
    private Long memberCount;
    // 직급 정보
    private String positionName;


    /**
     * 1:1 채팅방 목록 DTO 생성을 위한 정적 팩토리 메소드
     */
    public static ChatRoomListResponseDTO toDTO(
            ChatRoom room,
            String otherUserName,
            String otherUserProfile,
            String positionName,
            long unreadCount,
            Optional<ChatMessage> lastMessageOpt,
            long memberCount
    ) {
        String lastMessageContent = lastMessageOpt
                .map(ChatMessage::getContent)
                .orElse("채팅방이 생성 되었습니다."); // 메시지가 없으면 방 생성 시각

        // 마지막 메시지 시간 또는 방 생성 시간
        LocalDateTime lastTime = lastMessageOpt
                .map(ChatMessage::getCreatedAt)
                .orElse(room.getCreatedAt());  // 메시지가 없으면 방 생성 시간

        return ChatRoomListResponseDTO.builder()
                .chatRoomId(room.getChatRoomId())
                .name(otherUserName) // 상대방 이름
                .profile(otherUserProfile)
                .positionName(positionName)
                .unreadCount(unreadCount)
                .lastMessage(lastMessageContent)
                .lastMessageCreatedAt(lastTime)
                .isTeam(room.getIsTeam())
                .memberCount(memberCount)
                .build();
    }

    /**
     * 팀 채팅방 목록 DTO 생성을 위한 정적 팩토리 메소드
     */
    public static ChatRoomListResponseDTO toDTO(
            ChatRoom room,
            long unreadCount,
            Optional<ChatMessage> lastMessageOpt,
            long memberCount
    ) {
        String lastMessageContent = lastMessageOpt
                .map(ChatMessage::getContent)
                .orElse(room.getName() + "채팅방이 생성 되었습니다.");

        LocalDateTime lastTime = lastMessageOpt
                .map(ChatMessage::getCreatedAt)
                .orElse(room.getCreatedAt());

        return ChatRoomListResponseDTO.builder()
                .chatRoomId(room.getChatRoomId())
                .name(room.getName()) // 채팅방 이름
                .profile(null) // 팀방은 프로필/상태 없음
                .positionName(null)
                .unreadCount(unreadCount)
                .lastMessage(lastMessageContent)
                .lastMessageCreatedAt(lastTime)
                .isTeam(room.getIsTeam())
                .memberCount(memberCount)
                .build();
    }
}
