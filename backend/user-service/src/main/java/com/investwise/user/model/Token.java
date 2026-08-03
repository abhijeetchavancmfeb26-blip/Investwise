package com.investwise.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single-use, time-boxed token emailed to the user.
 * <p>
 * One table serves both email verification and password reset; the purpose column
 * distinguishes them and carries its own validity window. The original had two
 * near-identical entities.
 */
@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.TokenPurpose purpose;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static Token issue(User user, Enums.TokenPurpose purpose) {
        Token token = new Token();
        token.token = UUID.randomUUID().toString().replace("-", "");
        token.purpose = purpose;
        token.user = user;
        token.expiresAt = LocalDateTime.now().plusMinutes(purpose.validityMinutes());
        return token;
    }

    public boolean isUsable() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
}
