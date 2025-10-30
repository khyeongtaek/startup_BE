package org.goodee.startup_BE.employee.filter;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.employee.config.SecurityConfig;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.service.JwtService;
import org.goodee.startup_BE.employee.service.JwtUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtUserDetailsService jwtUserDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException,
            ExpiredJwtException, SignatureException, MalformedJwtException,
            ResourceNotFoundException, UsernameNotFoundException {

        final String servletPath = request.getServletPath();

        // --- 현재 경로가 permitAll 경로인지 확인 하고 해당 되면 건너뜀 ---
        boolean isPermitAll = false;
        for (String path : SecurityConfig.PERMIT_ALL_PATH) {
            if (pathMatcher.match(path, servletPath)) {
                isPermitAll = true;
                break;
            }
        }

        final String jwtToken = extractTokenFromCookies(request, "accessToken");

        if (jwtToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isPermitAll) {
            // 토큰이 있고 만료됐지만 스웨거에 접속해야 하는 경우 토큰 검증 건너뜀
            log.debug("Token found, but path {} is permitAll. Skipping token validation.", servletPath);
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Token found, path {} requires authentication. Validating token.", servletPath);

        // JWT 토큰에 포함된 사용자 이름 추출
        final String username = jwtService.extractUsername(jwtToken);

        // 검증이 필요한지 체크
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // JWT 토큰 검증을 위해 UserDetails 객체 생성
            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

            // JWT 토큰 검증
            if (jwtService.isValidToken(jwtToken, userDetails)) {

                // 검증 완료 시 인증 토큰 생성
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Security Context에 인증 토큰 저장
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Token validated successfully for user {}", username);
            }
        }

        // 다음 필터를 진행
        filterChain.doFilter(request, response);

    }

    private String extractTokenFromCookies(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}