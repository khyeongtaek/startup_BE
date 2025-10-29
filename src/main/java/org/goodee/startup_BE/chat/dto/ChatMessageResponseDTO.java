package org.goodee.startup_BE.chat.dto;

import lombok.*;
import org.goodee.startup_BE.chat.entity.ChatMessage;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ChatMessageResponseDTO {

    private Long chatMessageId;     // 메시지 PK
    private Long chatRoomId;        // 방 PK
    private Long employeeId;        // 보낸 사람(시스템 메시지면 null)
    private String senderName;      // 선택: 표시용(있으면 편리)
    private String content;         // 내용
    private LocalDateTime createdAt;// 생성 시각

    /** 엔티티 -> DTO 변환 */
    public static ChatMessageResponseDTO toDTO(ChatMessage chatMessage) {
        return ChatMessageResponseDTO.builder()
                .chatMessageId(chatMessage.getChatMessageId())
                .chatRoomId(chatMessage.getChatRoom().getChatRoomId())
                .employeeId(chatMessage.getEmployee() != null ? chatMessage.getEmployee().getEmployeeId() : null) // 시스템 메시지는 null
                .senderName(chatMessage.getEmployee() != null ? chatMessage.getEmployee().getName() : "SYSTEM")
                .content(chatMessage.getContent())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
