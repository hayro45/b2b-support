package com.hayrettindal.support.auth.application;

import com.hayrettindal.support.auth.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds,
        Environment environment
    ) {
        if (secret.isBlank() || (environment.matchesProfiles("prod") && secret.equals("ZGV2LXNlY3JldC1jaGFuZ2UtdGhpcy1pbi1wcm9kdWN0aW9uLTEyMw=="))) {
            throw new IllegalArgumentException("A private APP_JWT_SECRET is required in production");
        }
        if (expirationSeconds < 60 || expirationSeconds > 86400) throw new IllegalArgumentException("JWT expiry must be 60..86400 seconds");
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UUID userId, UUID organizationId, String email, UserRole role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
            .subject(email)
            .claims(Map.of(
                "uid", userId.toString(),
                "org", organizationId.toString(),
                "role", role.name()
            ))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
