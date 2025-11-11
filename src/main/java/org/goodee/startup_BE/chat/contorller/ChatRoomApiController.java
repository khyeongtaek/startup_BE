package org.goodee.startup_BE.chat.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.chat.dto.ChatMessageResponseDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomCreateRequestDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomListResponseDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomResponseDTO;
import org.goodee.startup_BE.chat.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ChatRoom API", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomApiController {

    private final ChatService chatService;

    // 채팅방 목록 조회 (UserList.jsx에서 사용)
    @Operation(summary = "채팅방 목록 조회")
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomListResponseDTO>> getRooms(
            Authentication authentication
    ) {
        String username = authentication.getName();
        List<ChatRoomListResponseDTO> rooms = chatService.findRoomsByUsername(username);
        return ResponseEntity.ok(rooms);
    }

    // 채팅방 생성
    @Operation(summary = "채팅방 생성")
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponseDTO> createRoom(
            Authentication authentication,
            @RequestBody ChatRoomCreateRequestDTO request
    ) {
        String username = authentication.getName();
        ChatRoomResponseDTO dto = chatService.createRoom(username, request.getDisplayName(), request.getInviteeEmployeeIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // 채팅방 초대
    @Operation(summary = "채팅방 초대")
    @PostMapping("/rooms/{roomId}/invite")
    public ResponseEntity<Void> invite(
            Authentication authentication,
            @PathVariable Long roomId,
            @RequestBody InviteRequest request
    ) {
        String username = authentication.getName();
        chatService.inviteToRoom(username, roomId, request.inviteeEmployeeIds);
        return ResponseEntity.noContent().build();
    }

    // 채팅방 나가기
    @Operation(summary = "채팅방 나가기")
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Void> leave(
            Authentication authentication,
            @PathVariable Long roomId
    ) {
        String username = authentication.getName();
        chatService.leaveRoom(username, roomId);
        return ResponseEntity.noContent().build();
    }

    // 메시지 목록 조회 (페이지네이션)
    @Operation(summary = "채팅방 메시지 페이지네이션")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageResponseDTO>> getMessages(
            Authentication authentication,
            @PathVariable Long roomId,
            Pageable pageable
    ) {
        String username = authentication.getName();
        Page<ChatMessageResponseDTO> page = chatService.getMessages(username, roomId, pageable);
        return ResponseEntity.ok(page);
    }

    // 마지막 읽은 메시지 갱신
    @Operation(summary = "마지막 읽은 메시지 갱신")
    @PatchMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> updateLastRead(
            Authentication authentication,
            @PathVariable Long roomId,
            @RequestParam("lastMessageId") Long lastMessageId
    ) {
        String username = authentication.getName();
        chatService.updateLastReadMessageId(username, roomId, lastMessageId);
        return ResponseEntity.noContent().build();
    }

    // Request DTOs
    public static class CreateRoomRequest {
        public String roomName;
        public List<Long> inviteeEmployeeIds;
    }

    public static class InviteRequest {
        public List<Long> inviteeEmployeeIds;
    }
}
