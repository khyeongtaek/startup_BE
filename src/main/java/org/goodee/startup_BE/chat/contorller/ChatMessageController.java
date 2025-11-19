package org.goodee.startup_BE.chat.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.chat.dto.MessageSendPayloadDTO;
import org.goodee.startup_BE.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Tag(name = "ChatMessage API", description = "채팅 메시지 관련 API")
@Controller
@RequiredArgsConstructor
@MessageMapping("/chat")
public class ChatMessageController {

    private final ChatService chatService;

    // STOMP: /app/chat/rooms/{roomId}/send 로 수신
    @Operation(summary = "메시지 보내기")
    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(
            Authentication authentication,
            @DestinationVariable("roomId") Long roomId,
            @Payload MessageSendPayloadDTO payload
    ) {
        if (authentication == null) return; // 인증되지 않은 소스 무시
        chatService.sendMessage(authentication.getName(), roomId, payload);
        // 브로드캐스트는 서비스에서 afterCommit으로 수행됨
    }
}
