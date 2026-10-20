package com.foodpassport.friend;

import com.foodpassport.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/**
 * One row per pair of users. Starts PENDING when "requester" asks; becomes ACCEPTED when "addressee" says yes.
 * Declining or unfriending simply deletes the row.
 */
@Entity
public class Friendship {

    public enum Status { PENDING, ACCEPTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;

    protected Friendship() {
    }

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public void accept() {
        this.status = Status.ACCEPTED;
    }

    /** Given one side of the friendship, return the other person. */
    public User other(Long userId) {
        return requester.getId().equals(userId) ? addressee : requester;
    }

    public boolean involves(Long userId) {
        return requester.getId().equals(userId) || addressee.getId().equals(userId);
    }

    public Long getId() { return id; }
    public User getRequester() { return requester; }
    public User getAddressee() { return addressee; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
