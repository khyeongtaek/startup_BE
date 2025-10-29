package org.goodee.startup_BE.chat.dto;

import lombok.*;
import org.goodee.startup_BE.chat.entity.ChatEmployee;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.employee.entity.Employee;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ChatEmployeeRequestDTO {

    private Long chatEmployeeId;
    private Long employeeId;
    private Long chatRoomId;
    private String displayName;
    private Long lastMessageId;

    public ChatEmployee toEntity(Employee employee, ChatRoom chatRoom, ChatMessage lastMessage) {
        String finalDisplayName = (displayName != null && !displayName.isBlank())
                ? displayName : (employee != null ? employee.getName() : "");
        return ChatEmployee.createChatEmployee(employee, chatRoom, finalDisplayName, lastMessage);
    }
}
