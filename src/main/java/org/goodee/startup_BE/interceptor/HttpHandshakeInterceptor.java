package org.goodee.startup_BE.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

            Cookie[] cookies = httpServletRequest.getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        // webSocket 세션 속성에 토큰 저장
                        attributes.put("accessToken", cookie.getValue());
                        log.debug("핸드셰이크 인터셉터: accessToken 쿠키 발견 및 세션에 저장");
                        break;
                    }
                }

            } else {
                log.warn("핸드셰이크 인터셉터: 쿠키를 찾을 수 없음");
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
