package com.foodpassport.user;

import com.foodpassport.auth.CurrentUser;
import com.foodpassport.common.ApiException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    public record MeResponse(Long id, String username, String email, java.time.Instant createdAt) {
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = users.findById(CurrentUser.id(jwt))
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        return new MeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}
