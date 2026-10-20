package com.foodpassport.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Every request carries a JWT. Its "sub" (subject) claim is the user's id, set by {@link TokenService}.
 * Spring has already checked the token's signature and expiry before our code runs.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
