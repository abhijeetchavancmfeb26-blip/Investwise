package com.investwise.user.security;

import com.investwise.user.model.Role;
import com.investwise.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Issues and validates JSON Web Tokens.
 * <p>
 * Refresh tokens were removed. The original stored them in a table, rotated them
 * on use and deduplicated concurrent refreshes in the browser — a great deal of
 * machinery to achieve what a longer-lived token achieves directly. The token now
 * lasts 8 hours, or 30 days when the user ticks "keep me signed in".
 * <p>
 * The token carries the user's name, email and tier, which is what removed the
 * need for a service-to-service lookup channel entirely.
 */
@Slf4j
@Component
public class JwtService {

    private final SecretKey key;
    private final long defaultValiditySeconds;
    private final long rememberMeValiditySeconds;

    public JwtService(@Value("${investwise.jwt.secret}") String secret,
                      @Value("${investwise.jwt.validity-seconds:28800}") long defaultValiditySeconds,
                      @Value("${investwise.jwt.remember-me-seconds:2592000}") long rememberMeValiditySeconds) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException | IllegalArgumentException ex) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("investwise.jwt.secret must decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.defaultValiditySeconds = defaultValiditySeconds;
        this.rememberMeValiditySeconds = rememberMeValiditySeconds;
    }

    public String generate(User user, boolean rememberMe) {
        long seconds = rememberMe ? rememberMeValiditySeconds : defaultValiditySeconds;
        Date now = new Date();

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + seconds * 1000))
                .claim("uid", user.getId())
                .claim("name", user.getFullName())
                .claim("tier", user.getTier().name())
                .claim("roles", user.getRoles().stream().map(Role::getName).toList())
                .signWith(key)
                .compact();
    }

    public long validitySeconds(boolean rememberMe) {
        return rememberMe ? rememberMeValiditySeconds : defaultValiditySeconds;
    }

    /** @return the caller, or null if the token is absent, expired or forged */
    @SuppressWarnings("unchecked")
    public AuthUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();

            Object roles = claims.get("roles");
            return new AuthUser(
                    claims.get("uid", Number.class).longValue(),
                    claims.getSubject(),
                    claims.get("name", String.class),
                    claims.get("tier", String.class),
                    roles instanceof List<?> list ? (List<String>) list : List.of());

        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            log.debug("Rejected token: {}", ex.getMessage());
            return null;
        }
    }
}
