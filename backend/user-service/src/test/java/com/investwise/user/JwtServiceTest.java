package com.investwise.user;

import com.investwise.user.model.Enums;
import com.investwise.user.model.Role;
import com.investwise.user.model.User;
import com.investwise.user.security.AuthUser;
import com.investwise.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1pbnZlc3R3aXNlLXVuaXQtdGVzdHMtMzJieXRlcw==";
    private static final String OTHER = "YW4tZW50aXJlbHktZGlmZmVyZW50LXNlY3JldC1rZXktZm9yLW5lZ2F0aXZlLXRlc3Rz";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 28800, 2592000);

        Set<Role> roles = new LinkedHashSet<>();
        roles.add(new Role("ROLE_USER"));
        roles.add(new Role("ROLE_ADMIN"));

        user = User.builder().id(42L).firstName("Priya").lastName("Nair")
                .email("priya.nair@example.com").tier(Enums.Tier.PREMIUM).roles(roles).build();
    }

    @Test
    @DisplayName("round-trips every claim the platform depends on")
    void roundTrips() {
        AuthUser parsed = jwtService.parse(jwtService.generate(user, false));

        assertThat(parsed).isNotNull();
        assertThat(parsed.id()).isEqualTo(42L);
        assertThat(parsed.email()).isEqualTo("priya.nair@example.com");
        assertThat(parsed.name()).isEqualTo("Priya Nair");
        assertThat(parsed.tier()).isEqualTo("PREMIUM");
        assertThat(parsed.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(parsed.isPremium()).isTrue();
        assertThat(parsed.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("carrying name and tier in the token is what removes the need for a lookup call")
    void tokenCarriesEverythingDownstreamNeeds() {
        AuthUser parsed = jwtService.parse(jwtService.generate(user, false));

        // The Investment Service needs exactly these to render reports and payments
        assertThat(parsed.name()).isNotBlank();
        assertThat(parsed.email()).isNotBlank();
        assertThat(parsed.tier()).isNotBlank();
    }

    @Test
    @DisplayName("rememberMe extends the validity window")
    void rememberMeIsLonger() {
        assertThat(jwtService.validitySeconds(true))
                .isGreaterThan(jwtService.validitySeconds(false));
    }

    @Test
    @DisplayName("rejects a token signed with a different key")
    void rejectsForeignSignature() {
        JwtService attacker = new JwtService(OTHER, 28800, 2592000);
        assertThat(jwtService.parse(attacker.generate(user, false))).isNull();
    }

    @Test
    @DisplayName("rejects an expired token")
    void rejectsExpired() {
        JwtService shortLived = new JwtService(SECRET, -60, -60);
        assertThat(jwtService.parse(shortLived.generate(user, false))).isNull();
    }

    @Test
    @DisplayName("returns null rather than throwing on malformed input")
    void rejectsGarbage() {
        assertThat(jwtService.parse("not-a-jwt")).isNull();
        assertThat(jwtService.parse("")).isNull();
        assertThat(jwtService.parse("a.b.c")).isNull();
    }

    @Test
    @DisplayName("refuses to start with a secret shorter than HS256 requires")
    void refusesWeakSecret() {
        assertThatThrownBy(() -> new JwtService("short", 28800, 2592000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
