package org.goodee.startup_BE.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@Order(Integer.MIN_VALUE) // 모든 필터 중 가장 먼저 실행되도록 순서 지정
@RequiredArgsConstructor
public class ExceptionHandlerFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 다음 필터 체인을 실행
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            // 토큰 만료
            log.warn("Token has expired: {}", e.getMessage());
            setErrorResponse(response, HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
        } catch (SignatureException | MalformedJwtException | IllegalArgumentException e) {
            // 토큰 형식 또는 서명 오류
            log.warn("Invalid JWT token: {}", e.getMessage());
            setErrorResponse(response, HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다.");
        } catch (ResourceNotFoundException | UsernameNotFoundException e) {
            // 사용자 조회 실패
            log.warn("User not found during authentication: {}", e.getMessage());
            // 클라이언트에게는 401과 "토큰이 유효하지 않습니다"로 통일하는 것이 보안상 더 나을 수 있음
            setErrorResponse(response, HttpStatus.UNAUTHORIZED, "토큰에 해당하는 사용자를 찾을 수 없습니다.");
        } catch (Exception e) {
            log.error("Unhandled exception in filter chain", e);
            setErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
        }
    }

    private void setErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        APIResponseDTO<String> errorResponse = APIResponseDTO.<String>builder()
                .message(message)
                .data(status.getReasonPhrase())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}