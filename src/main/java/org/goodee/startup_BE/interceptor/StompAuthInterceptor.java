package org.goodee.startup_BE.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.employee.service.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService; //
    private final UserDetailsService userDetailsService; // (JwtUserDetailsService가 주입됨)

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // STOMP 'CONNECT' 명령일 때만 인증 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("STOMP CONNECT 요청. 헤더에서 JWT 토큰 인증을 시도합니다.");

            // 프론트엔드에서 보낸 'Authorization' 헤더 추출
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwtToken = authHeader.substring(7);
                try {
                    // JwtService를 사용해 사용자 이름 추출
                    String username = jwtService.extractUsername(jwtToken); //

                    if (username != null) {
                        // JwtUserDetailsService를 사용해 UserDetails 로드
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username); //

                        // JwtService로 토큰 유효성 검증
                        if (jwtService.isValidToken(jwtToken, userDetails)) { //

                            // Spring Security Authentication 객체 생성
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                            // STOMP 세션의 'user' (Principal)로 설정
                            accessor.setUser(authToken);
                            log.debug("STOMP 인증 성공. 사용자: {}", username);
                        } else {
                            log.warn("STOMP 인증 실패: 유효하지 않은 토큰");
                        }
                    }
                } catch (Exception e) {
                    log.warn("STOMP 인증 중 예외 발생: {}", e.getMessage());
                }
            } else {
                log.warn("STOMP 인증 실패: Authorization 헤더가 없거나 Bearer 타입이 아닙니다.");
            }
        }
        return message;
    }
}