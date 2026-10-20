package com.foodpassport.friend;

import com.foodpassport.auth.CurrentUser;
import com.foodpassport.friend.FriendDtos.FriendRequestCreate;
import com.foodpassport.friend.FriendDtos.FriendRequestResponse;
import com.foodpassport.friend.FriendDtos.PendingRequests;
import com.foodpassport.user.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friends;

    public FriendController(FriendService friends) {
        this.friends = friends;
    }

    @GetMapping
    public List<UserSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return friends.friends(CurrentUser.id(jwt));
    }

    @DeleteMapping("/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfriend(@AuthenticationPrincipal Jwt jwt, @PathVariable Long friendId) {
        friends.unfriend(CurrentUser.id(jwt), friendId);
    }

    @GetMapping("/requests")
    public PendingRequests pending(@AuthenticationPrincipal Jwt jwt) {
        return friends.pending(CurrentUser.id(jwt));
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestResponse send(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FriendRequestCreate request) {
        return friends.sendRequest(CurrentUser.id(jwt), request.username());
    }

    @PostMapping("/requests/{id}/accept")
    public FriendRequestResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return friends.accept(CurrentUser.id(jwt), id);
    }

    @DeleteMapping("/requests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        friends.deleteRequest(CurrentUser.id(jwt), id);
    }
}
