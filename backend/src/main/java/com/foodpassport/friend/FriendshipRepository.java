package com.foodpassport.friend;

import com.foodpassport.friend.Friendship.Status;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** The friendship between two users, whichever of them sent the request. */
    @Query("""
            select f from Friendship f
            where (f.requester.id = :a and f.addressee.id = :b)
               or (f.requester.id = :b and f.addressee.id = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("""
            select f from Friendship f join fetch f.requester join fetch f.addressee
            where f.status = :status and (f.requester.id = :userId or f.addressee.id = :userId)
            """)
    List<Friendship> findAllFor(@Param("userId") Long userId, @Param("status") Status status);

    /** Of the given user ids, which are accepted friends of userId? */
    @Query("""
            select case when f.requester.id = :userId then f.addressee.id else f.requester.id end
            from Friendship f
            where f.status = com.foodpassport.friend.Friendship.Status.ACCEPTED
              and ((f.requester.id = :userId and f.addressee.id in :candidates)
                or (f.addressee.id = :userId and f.requester.id in :candidates))
            """)
    List<Long> findFriendIdsAmong(@Param("userId") Long userId, @Param("candidates") Collection<Long> candidates);
}
