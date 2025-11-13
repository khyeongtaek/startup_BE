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

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {
  
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    
    // STOMP 'CONNECT' 명령일 때만 인증 처리
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      log.debug("STOMP CONNECT 요청. 세션 속성에서 JWT 토큰 인증을 시도합니다.");
      
      // 핸드셰이크 시 저장된 세션 속성(attributes)에서 토큰 추출
      Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
      String jwtToken = null;
      
      if (sessionAttributes != null) {
        jwtToken = (String) sessionAttributes.get("accessToken");
      }
      
      // 토큰 존재 여부 검사
      if (jwtToken != null) {
        try {
          // JwtService를 사용해 사용자 이름 추출
          String username = jwtService.extractUsername(jwtToken);
          
          if (username != null) {
            // JwtUserDetailsService를 사용해 UserDetails 로드
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            
            // JwtService로 토큰 유효성 검증
            if (jwtService.isValidToken(jwtToken, userDetails)) {
              
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
        log.warn("STOMP 인증 실패: 세션에서 accessToken을 찾을 수 없습니다. (쿠키 누락)");
      }
    }
    return message;
  }
}