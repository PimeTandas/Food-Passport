package com.foodpassport.auth;

import com.foodpassport.user.UserSummary;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.]{3,30}$",
                    message = "3-30 characters: letters, numbers, '_' or '.'") String username,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    /** The client stores the token and sends it as "Authorization: Bearer <token>" on every request. */
    public record TokenResponse(String token, UserSummary user) {
    }
}
