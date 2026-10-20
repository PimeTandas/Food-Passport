package com.foodpassport.auth;

import com.foodpassport.auth.AuthDtos.LoginRequest;
import com.foodpassport.auth.AuthDtos.RegisterRequest;
import com.foodpassport.auth.AuthDtos.TokenResponse;
import com.foodpassport.common.ApiException;
import com.foodpassport.user.User;
import com.foodpassport.user.UserRepository;
import com.foodpassport.user.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public TokenResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("That email is already registered");
        }
        if (users.existsByUsernameIgnoreCase(request.username())) {
            throw ApiException.conflict("That username is taken");
        }
        User user = users.save(new User(email, request.username(), passwordEncoder.encode(request.password())));
        return new TokenResponse(tokens.issue(user), UserSummary.from(user));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // Same error for "no such email" and "wrong password", so attackers can't probe which emails exist.
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Wrong email or password"));
        return new TokenResponse(tokens.issue(user), UserSummary.from(user));
    }
}
