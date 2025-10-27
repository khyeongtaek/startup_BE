package org.goodee.startup_BE.chat.dto;

import lombok.*;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.employee.entity.Employee;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ChatRoomRequestDTO {

    private Long employeeId;
    private String displayName;
    private Boolean isTeam;

    public ChatRoom toEntity(Employee employee) {
        return ChatRoom.createChatRoom(employee, displayName, isTeam);
    }
}
