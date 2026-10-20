package com.foodpassport.user;

/** The public face of a user: safe to show to other people. */
public record UserSummary(Long id, String username) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getUsername());
    }
}
