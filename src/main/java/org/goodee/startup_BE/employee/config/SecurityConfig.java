package org.goodee.startup_BE.employee.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.exception.ExceptionHandlerFilter;
import org.goodee.startup_BE.employee.enums.Role;
import org.goodee.startup_BE.employee.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final ExceptionHandlerFilter exceptionHandlerFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 허용 경로
    public static final String[] PERMIT_ALL_PATH = {
            // 인증관련 경로 허용
            "/api/auth/login",
            "/api/auth/refresh",

            //스웨거 관련 경로 허용
            "/swagger-ui/**",
            "/v3/api-docs/**",

    };

    @Value("${allowed.origin}")
    private String allowedOrigin;

    //----- 시큐리티 필터 체인
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 요청(주소)별 인증 설정
                .authorizeHttpRequests(auth -> auth
                                // 허용될 주소 배열
                                .requestMatchers(PERMIT_ALL_PATH)
                                .permitAll()
                                                 
                                // /api/auth/signup은 "ROLE_ADMIN" 권한을 가진 사용자만 허용
                                .requestMatchers("/api/auth/signup")
                                .hasAuthority(Role.ROLE_ADMIN.name())

                                // 나머지 모든 요청은 인증 필요
                                .anyRequest()
                                .authenticated()
                )

                // 세션 설정 (JWT 사용 시 STATELESS로 설정: 세션 사용 안 함)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CORS 설정
                .cors(configurer -> configurer.configurationSource(corsConfigurationSource()))

                // 예외 처리 설정 추가
                .exceptionHandling(exceptions -> exceptions
                        // 인증 실패 시(401) 호출될 엔트리 포인트를 람다식으로 직접 정의
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 응답 상태를 401 (Unauthorized)로 설정
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            // 응답 컨텐츠 타입을 JSON으로 설정
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            // 응답 인코딩을 UTF-8로 설정
                            response.setCharacterEncoding("UTF-8");

                            // 예외 로그 기록
                            log.warn("Unauthorized request: {}", request.getRequestURI());
                            log.warn("Exception: {}", authException.getMessage());

                            // APIResponseDTO를 사용하여 에러 응답 본문 구성
                            APIResponseDTO<String> errorResponse = APIResponseDTO.<String>builder()
                                    .message(authException.getMessage())
                                    .data(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                    .build();

                            // ObjectMapper를 사용해 DTO를 JSON 문자열로 변환하고 응답에 써줌
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                )

                // JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 배치
                // UsernamePasswordAuthenticationFilter는 폼 로그인 시(POST /login) 사용하는 필터
                // JwtAuthenticationFilter가 먼저 동작하고 인증에 성공하면 UsernamePasswordAuthenticationFilter가 실행되지 않는다.
                // UsernamePasswordAuthenticationFilter 앞에 JwtAuthenticationFilter를 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // JwtAuthenticationFilter 앞에 ExceptionHandlerFilter를 추가 (따라서 가장 먼저 실행됨)
                .addFilterBefore(exceptionHandlerFilter, JwtAuthenticationFilter.class);

        return http.build();

    }

    //----- CORS 설정
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // React App 허용
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigin));
        // HTTP 메소드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 인증 정보 허용 (JWT 토큰)
        configuration.setAllowCredentials(true); // ← 쿠키 허용
        // 모든 경로에 configuration 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        // 반환
        return source;
    }

    //----- spring security가 지원하는 비밀번호 암호화
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}