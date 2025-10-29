package org.goodee.startup_BE.chat.dto;

import lombok.*;
import org.goodee.startup_BE.chat.entity.ChatEmployee;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.employee.entity.Employee;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatEmployeeResponseDTO {

    private Long chatEmployeeId;
    private Long employeeId;
    private Long chatRoomId;
    private String displayName;
    private Long lastMessageId;
    private Boolean isNotification;

    public ChatEmployeeResponseDTO toDTO(ChatEmployee chatEmployee) {
        return ChatEmployeeResponseDTO.builder()
                .chatEmployeeId(chatEmployee.getChatEmployeeId())
                .employeeId(chatEmployee.getEmployee().getEmployeeId())
                .chatRoomId(chatEmployee.getChatRoom().getChatRoomId())
                .displayName(chatEmployee.getDisplayName())
                .lastMessageId(chatEmployee.getLastReadMessage() != null ? chatEmployee.getLastReadMessage().getChatMessageId() : null)
                .isNotification(chatEmployee.getIsNotify())
                .build();
    }
}
