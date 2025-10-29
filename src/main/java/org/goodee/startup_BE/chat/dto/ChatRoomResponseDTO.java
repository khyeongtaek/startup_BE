package org.goodee.startup_BE.chat.dto;

import lombok.*;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ChatRoomResponseDTO {

    private Long chatRoomId;        // 방 PK
    private Long employeeId;        // 생성자(개설자) ID
    private String displayName;     // 방 이름
    private Boolean isTeam;         // 팀방 여부
    private LocalDateTime createdAt;// 생성 시각

    /** 엔티티 -> DTO 변환 */
    public static ChatRoomResponseDTO toDTO(ChatRoom chatRoom) {
        return ChatRoomResponseDTO.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .employeeId(chatRoom.getEmployee().getEmployeeId())
                .displayName(chatRoom.getName())
                .isTeam(chatRoom.getIsTeam())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
