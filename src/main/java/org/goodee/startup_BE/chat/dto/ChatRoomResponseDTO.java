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
    private String name;        // 생성자(개설자) ID
    private String displayName;     // 방 이름
    private Boolean isTeam;         // 팀방 여부
    private LocalDateTime createdAt;// 생성 시각
    private Long memberCount;       // 채팅방에 참여 중인 인원 수
    private String profileImg;      // 프로필 이미지
    private String positionName;    // 직급

    /** 엔티티 -> DTO 변환 */
    public static ChatRoomResponseDTO toDTO(ChatRoom chatRoom, Long memberCount) {

        Employee owner = chatRoom.getEmployee();

        // 직급 명칭 가져오기
        String positionName = "";
        if (owner != null && owner.getPosition() != null) {
            positionName = owner.getPosition().getValue1();
        }

        // 프로필 이미지 가져오기
        String profileImg = (owner != null) ? owner.getProfileImg() : null;

        return ChatRoomResponseDTO.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .name(owner != null ? owner.getName() : "정보 없음")
                .displayName(chatRoom.getIsTeam() ? chatRoom.getName() : (owner != null ? owner.getName() : "정보 없음"))
                .isTeam(chatRoom.getIsTeam())
                .createdAt(chatRoom.getCreatedAt())
                .memberCount(memberCount)
                .profileImg(profileImg)
                .positionName(positionName)
                .build();
    }
}
