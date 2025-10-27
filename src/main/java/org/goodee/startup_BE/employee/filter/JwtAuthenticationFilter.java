package org.goodee.startup_BE.employee.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.employee.service.JwtService;
import org.goodee.startup_BE.employee.service.JwtUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 요청 헤더 Authorization에 포함된 JWT 토큰을 꺼내서
    // JWT 토큰의 유효성을 검증한 뒤
    // 인증 토큰을 발행하여 SecurityContext에 저장.

    private final JwtService jwtService;
    private final JwtUserDetailsService jwtUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        if (request.getServletPath().equals("/api/auth/login")) {
            // 로그인 요청일 경우, 토큰 검증 로직을 건너뛰고 다음 필터로 진행
            filterChain.doFilter(request, response);
            return;
        }
        //----- Authorization 헤더에서 JWT 토큰 추출

        // 1. Authorization 헤더 추출
        final String authHeader = request.getHeader("Authorization");

        // 2. Authorization 헤더가 없거나, Bearer 토큰이 아니면 다음 필터 진행
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " 이후 부분(JWT 토큰) 추출
        final String jwtToken = authHeader.substring("Bearer ".length());


        //----- 사용자 이름과 토큰 만료시간을 이용해 JWT 토큰 검증

        // 1. JWT 토큰에 포함된 사용자 이름 추출
        final String username = jwtService.extractUsername(jwtToken);

        // 2. 검증이 필요한지 체크
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 3. JWT 토큰 검증을 위해 UserDetails 객체 생성
            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

            // 4. JWT 토큰 검증
            if (jwtService.isValidToken(jwtToken, userDetails)) {

                // 5. 검증 완료 시 인증 토큰 생성
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,                  // principal (사용자 정보)
                        null,                         // credentials (비밀번호)
                        userDetails.getAuthorities()  // authorities (권한)
                );

                // 6. 인증 토큰에 요청 세부사항 설정
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Security Context에 인증 토큰 저장
                SecurityContextHolder.getContext().setAuthentication(authToken);

            }

        }

        // 다음 필터를 진행
        filterChain.doFilter(request, response);

    }

}
