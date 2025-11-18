package org.goodee.startup_BE.config;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.interceptor.HttpHandshakeInterceptor;
import org.goodee.startup_BE.interceptor.StompAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;
    private final HttpHandshakeInterceptor httpHandshakeInterceptor;

    @Value("${allowed.origin}")
    private String allowedOrigin;

    /**
     * 클라이언트가 WebSocket에 연결할 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigin) // CORS 설정을 대신 해주며, 운영 환경에서는 보안을 위해 정확한 도메인을 지정 해줘야 함
                .addInterceptors(httpHandshakeInterceptor)  // 핸드셰이크 인터셉터
                .withSockJS();  // SockJS를 사용하여 Websocket을 지원하지 않는 브라우저를 대비 (공부용 이라서 남겨둠)
    }

    /**
     * 인메모리 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 클라이언트가 메시지를 보낼 때(SEND) 사용할 경로의 접두사 (수신)
        registry.setApplicationDestinationPrefixes("/app");

        // 서버에서 클라이언트에게 메시지를 푸시할 때 사용할 브로커의 접두사입니다. (공개, 개인 구독)
        // topic은 1:N, queue는 1:1 메시징에 사용됩니다.
        // Simple Broker를 사용하여 인메모리 메시지 브로커를 활성화합니다.
        registry.enableSimpleBroker("/topic", "/queue");

        // 서버가 개별 사용자에게 푸시할 때 사용하는 접두사 설정입니다. (사용자 개인)
        // /user/queue/noti 경로로 최종적으로 변환되어 해당 사용자에게만 전달됩니다.
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 클라이언트의 INBOUND 채널을 구성하는 메소드
     * 여기서 커스텀 인터셉터를 등록하여 인증을 처리 한다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
