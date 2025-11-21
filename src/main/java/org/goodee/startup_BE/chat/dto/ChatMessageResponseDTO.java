package org.goodee.startup_BE.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.enums.OwnerType;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ChatMessageResponseDTO {

    private Long chatMessageId;     // 메시지 PK
    private Long chatRoomId;        // 방 PK
    private Long employeeId;        // 보낸 사람(시스템 메시지면 null)
    private String senderName;      // 선택: 표시용(있으면 편리)
    private String content;         // 내용
    private LocalDateTime createdAt;// 생성 시각

    private List<AttachmentFileResponseDTO> attachments;
    private OwnerType messageType;

    @Builder.Default
    private long unreadCount = 0;  // 미읽음 카운트

    /** 엔티티 -> DTO 변환 */
    public static ChatMessageResponseDTO toDTO(ChatMessage chatMessage) {

        String senderName;
        Long employeeId;

        if (chatMessage.getMessageType() == OwnerType.CHAT_SYSTEM) {
            senderName = "SYSTEM";
            employeeId = null;
        } else if (chatMessage.getEmployee() == null) {
            senderName = "정보 없음";
            employeeId = null;
        } else {
            senderName = chatMessage.getEmployee().getName();
            employeeId = chatMessage.getEmployee().getEmployeeId();
        }
        return ChatMessageResponseDTO.builder()
                .chatMessageId(chatMessage.getChatMessageId())
                .chatRoomId(chatMessage.getChatRoom().getChatRoomId())
                .employeeId(employeeId)
                .senderName(senderName)
                .content(chatMessage.getContent())
                .createdAt(chatMessage.getCreatedAt())
                .messageType(chatMessage.getMessageType())
                .build();
    }
}
