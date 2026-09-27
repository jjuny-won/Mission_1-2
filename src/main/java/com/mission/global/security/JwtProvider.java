package com.mission.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long  expirationMs;

    public JwtProvider(@Value("${custom.jwt.secret}") String secret,
                       @Value("${custom.jwt.expiration-ms}") long expirationMs) {
        if (secret.isBlank()) {
            this.key = Jwts.SIG.HS256.key().build();
            log.warn("JWT_SECRET이 없어 임의 키를 생성합니다. 재시작하면 기존 토큰은 무효가 됩니다.");
        } else {
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
        this.expirationMs = expirationMs;
    }

    public String createAccessToken(Long memberId){
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    // 유효한 경우 ID, 위조 및 만료시 null return
    public Long getMemberId(String token){
        try{
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
