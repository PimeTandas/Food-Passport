package com.foodpassport.friend;

import com.foodpassport.common.ApiException;
import com.foodpassport.friend.FriendDtos.FriendRequestResponse;
import com.foodpassport.friend.FriendDtos.PendingRequests;
import com.foodpassport.friend.Friendship.Status;
import com.foodpassport.user.User;
import com.foodpassport.user.UserRepository;
import com.foodpassport.user.UserSummary;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FriendService {

    private final FriendshipRepository friendships;
    private final UserRepository users;

    public FriendService(FriendshipRepository friendships, UserRepository users) {
        this.friendships = friendships;
        this.users = users;
    }

    public FriendRequestResponse sendRequest(Long meId, String username) {
        User me = users.getReferenceById(meId);
        User them = users.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> ApiException.notFound("No user called " + username));
        if (them.getId().equals(meId)) {
            throw ApiException.badRequest("You can't friend yourself");
        }

        var existing = friendships.findBetween(meId, them.getId());
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == Status.ACCEPTED) {
                throw ApiException.conflict("You're already friends");
            }
            if (f.getRequester().getId().equals(meId)) {
                throw ApiException.conflict("Friend request already sent");
            }
            // They already asked us - sending one back means "yes".
            f.accept();
            return FriendRequestResponse.from(f);
        }
        return FriendRequestResponse.from(friendships.save(new Friendship(me, them)));
    }

    public FriendRequestResponse accept(Long meId, Long requestId) {
        Friendship f = findPending(requestId);
        if (!f.getAddressee().getId().equals(meId)) {
            throw ApiException.forbidden("Only the person who received the request can accept it");
        }
        f.accept();
        return FriendRequestResponse.from(f);
    }

    /** Decline (if it was sent to you) or cancel (if you sent it). */
    public void deleteRequest(Long meId, Long requestId) {
        Friendship f = findPending(requestId);
        if (!f.involves(meId)) {
            throw ApiException.notFound("Friend request " + requestId + " not found");
        }
        friendships.delete(f);
    }

    public void unfriend(Long meId, Long friendId) {
        Friendship f = friendships.findBetween(meId, friendId)
                .filter(x -> x.getStatus() == Status.ACCEPTED)
                .orElseThrow(() -> ApiException.notFound("You're not friends with user " + friendId));
        friendships.delete(f);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> friends(Long meId) {
        return friendships.findAllFor(meId, Status.ACCEPTED).stream()
                .map(f -> UserSummary.from(f.other(meId)))
                .sorted(Comparator.comparing(UserSummary::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public PendingRequests pending(Long meId) {
        List<Friendship> all = friendships.findAllFor(meId, Status.PENDING);
        return new PendingRequests(
                all.stream().filter(f -> f.getAddressee().getId().equals(meId)).map(FriendRequestResponse::from).toList(),
                all.stream().filter(f -> f.getRequester().getId().equals(meId)).map(FriendRequestResponse::from).toList());
    }

    /** Throws unless every id in userIds is an accepted friend of meId. */
    @Transactional(readOnly = true)
    public void requireAllFriends(Long meId, Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        Set<Long> notFriends = new HashSet<>(userIds);
        notFriends.removeAll(friendships.findFriendIdsAmong(meId, userIds));
        if (!notFriends.isEmpty()) {
            throw ApiException.badRequest("You can only tag friends. Not friends with user(s): " + notFriends);
        }
    }

    private Friendship findPending(Long requestId) {
        return friendships.findById(requestId)
                .filter(f -> f.getStatus() == Status.PENDING)
                .orElseThrow(() -> ApiException.notFound("Friend request " + requestId + " not found"));
    }
}
