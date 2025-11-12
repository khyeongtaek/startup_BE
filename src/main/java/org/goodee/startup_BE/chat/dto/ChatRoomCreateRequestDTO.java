package org.goodee.startup_BE.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatRoomCreateRequestDTO {

    // 프론트의 roomName을 받을 필드
    private String displayName;
    private List<Long> inviteeEmployeeIds;
}
