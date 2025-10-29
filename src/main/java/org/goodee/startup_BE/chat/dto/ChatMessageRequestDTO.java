package org.goodee.startup_BE.chat.dto;

import lombok.*;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.employee.entity.Employee;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ChatMessageRequestDTO {

    private Long chatRoomId;
    private Long employeeId;
    private String content;

    public ChatMessage toEntity(ChatRoom chatRoom, Employee employee) {
        return ChatMessage.createChatMessage(chatRoom, employee, content);
    }
}
