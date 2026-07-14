package com.campus.trade.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {
    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey secretKey;
    private final long expirationMillis;
    private final Clock clock;

    @Autowired
    public JwtUtils(@Value("${app.jwt.secret}") String base64Secret,
                    @Value("${app.jwt.expiration-millis:86400000}") long expirationMillis) {
        this(base64Secret, expirationMillis, Clock.systemUTC());
    }

    JwtUtils(String base64Secret, long expirationMillis, Clock clock) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMillis = expirationMillis;
        this.clock = clock;
    }

    public String generateToken(Long userId, String role) {
        Instant issuedAt = clock.instant();
        Instant expiration = issuedAt.plusMillis(expirationMillis);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(USER_ID_CLAIM, userId)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return parseToken(token).get(USER_ID_CLAIM, Long.class);
    }

    public String getRole(String token) {
        return parseToken(token).get(ROLE_CLAIM, String.class);
    }
}
