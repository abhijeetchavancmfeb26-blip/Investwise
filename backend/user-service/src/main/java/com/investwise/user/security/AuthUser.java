package com.investwise.user.security;

import java.util.List;

/**
 * The authenticated caller, rebuilt from JWT claims on every request.
 * <p>
 * A plain record used directly as the Spring Security principal. The original
 * had a class implementing {@code UserDetails} with eight boilerplate methods,
 * none of which were meaningful once authentication moved into the service.
 */
public record AuthUser(Long id, String email, String name, String tier, List<String> roles) {

    public boolean isPremium() {
        return tier != null && !"FREE".equals(tier);
    }

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
}
