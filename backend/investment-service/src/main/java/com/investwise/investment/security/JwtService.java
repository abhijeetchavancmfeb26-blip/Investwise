package com.investwise.investment.security;

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
import java.util.List;

/**
 * Verification-only counterpart of the User Service's token service.
 * <p>
 * This service holds the shared key and nothing more, which keeps issuance
 * authority in exactly one place. Because the token already carries the user's
 * name, email and tier, there is no service-to-service lookup channel at all —
 * the original's internal REST client, internal controller, shared API key and
 * two projection DTOs all disappeared with it.
 */
@Slf4j
@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${investwise.jwt.secret}") String secret) {
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
