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
        Employee employee = chatEmployee.getEmployee();

        return ChatEmployeeResponseDTO.builder()
                .chatEmployeeId(chatEmployee.getChatEmployeeId())
                .employeeId(employee != null ? employee.getEmployeeId() : null)
                .chatRoomId(chatEmployee.getChatRoom().getChatRoomId())
                .displayName(employee != null ? chatEmployee.getDisplayName() : "정보 없음")
                .lastMessageId(chatEmployee.getLastReadMessage() != null ? chatEmployee.getLastReadMessage().getChatMessageId() : null)
                .isNotification(chatEmployee.getIsNotify())
                .build();
    }
}
