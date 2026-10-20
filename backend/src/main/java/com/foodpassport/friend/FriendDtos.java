package com.foodpassport.friend;

import com.foodpassport.user.UserSummary;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class FriendDtos {

    private FriendDtos() {
    }

    public record FriendRequestCreate(@NotBlank String username) {
    }

    public record FriendRequestResponse(Long id, UserSummary from, UserSummary to, String status, Instant createdAt) {

        static FriendRequestResponse from(Friendship f) {
            return new FriendRequestResponse(f.getId(), UserSummary.from(f.getRequester()),
                    UserSummary.from(f.getAddressee()), f.getStatus().name(), f.getCreatedAt());
        }
    }

    public record PendingRequests(java.util.List<FriendRequestResponse> incoming,
                                  java.util.List<FriendRequestResponse> outgoing) {
    }
}
