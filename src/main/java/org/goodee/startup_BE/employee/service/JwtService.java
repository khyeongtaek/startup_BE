package org.goodee.startup_BE.employee.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    //------ application.properties 등록한 JWT 정보 가져오기

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    //----- 서명키 생성 (JWT 토큰 생성 시 필요한 정보. application.properties에 등록한 jwt.secret-key를 이용해 생성한 SecretKey 객체를 의미)
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    //----- JWT 토큰 생성
    public String generateToken(
            Map<String, Object> claims,  // 클레임
            UserDetails userDetails      // 사용자
    ) {
        return Jwts.builder()
                .claims(claims)  // JWT 토큰의 Payload 구성
                .subject(userDetails.getUsername())  // 토큰제목(발행한 사람으로 처리)
                .issuedAt(Date.from(Instant.now()))  // 발행일시
                .expiration(Date.from(Instant.now().plus(jwtExpiration, ChronoUnit.MILLIS)))  // 만료일시
                .signWith(getSignInKey())  // 서명키
                .compact();
    }


    //----- JWT 토큰에서 클레임 추출
    private Claims extractAllClaims(String jwtToken) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }

    // 2. 특정 클레임만 추출
    public <T> T extractClaim(String jwtToken, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(jwtToken));
    }

    // 3-1. JWT 토큰에서 사용자 이름 추출
    public String extractUsername(String jwtToken) {
        return extractClaim(jwtToken, Claims::getSubject);
    }

    // 3-2. JWT 토큰에서 만료시간 추출
    public Date extractExpiration(String jwtToken) {
        return extractClaim(jwtToken, Claims::getExpiration);
    }


    //----- JWT 토큰 유효성 검증
    public boolean isValidToken(String jwtToken, UserDetails userDetails) {
        try {
            // 사용자 일치 여부 + 만료시간 체크
            final String username = this.extractUsername(jwtToken);
            final Date expiration = this.extractExpiration(jwtToken);
            return (username.equals(userDetails.getUsername()) && expiration.after(Date.from(Instant.now())));
        } catch (Exception e) {
            return false;  // JWT 토큰에서 특정 클레임을 가져오기 실패
        }
    }

}
