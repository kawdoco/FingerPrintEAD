package com.bcilab.attendance.security;

import com.bcilab.attendance.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/** Issues and validates the JWT used by admin/coordinator sessions on the React dashboard. */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getJwt().getExpirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(username)
                .claims(Map.of("role", role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
