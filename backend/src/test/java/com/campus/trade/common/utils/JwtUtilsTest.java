package com.campus.trade.common.utils;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {
    private static final String SECRET =
            "Y2FtcHVzLXRyYWRlLXRlc3Qtc2VjcmV0LWtleS1jaGFuZ2UtbWU=";

    @Test
    void generatesAndParsesTokenClaims() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-14T08:00:00Z"), ZoneOffset.UTC);
        JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000, clock);

        String token = jwtUtils.generateToken(42L, "ADMIN");

        assertThat(jwtUtils.isTokenValid(token)).isTrue();
        assertThat(jwtUtils.getUserId(token)).isEqualTo(42L);
        assertThat(jwtUtils.getRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void rejectsExpiredToken() {
        Clock issuedAt = Clock.fixed(Instant.parse("2026-07-14T08:00:00Z"), ZoneOffset.UTC);
        String token = new JwtUtils(SECRET, 1_000, issuedAt).generateToken(1L, "USER");
        Clock afterExpiration = Clock.fixed(Instant.parse("2026-07-14T08:00:02Z"), ZoneOffset.UTC);
        JwtUtils verifier = new JwtUtils(SECRET, 1_000, afterExpiration);

        assertThat(verifier.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> verifier.parseToken(token)).isInstanceOf(ExpiredJwtException.class);
    }
}
