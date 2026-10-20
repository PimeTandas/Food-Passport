package com.foodpassport.stamp;

import com.foodpassport.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/**
 * A stamp on one person's country page. Dated automatically when created.
 * Holds the parts that are personal: your own rating and notes about the food entry.
 */
@Entity
public class Stamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private FoodEntry foodEntry;

    private Instant stampedAt;

    private Integer rating;

    private String notes;

    protected Stamp() {
    }

    public Stamp(User owner, FoodEntry foodEntry, Instant stampedAt) {
        this.owner = owner;
        this.foodEntry = foodEntry;
        this.stampedAt = stampedAt;
        foodEntry.getStamps().add(this);
    }

    public void review(Integer rating, String notes) {
        this.rating = rating;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public FoodEntry getFoodEntry() { return foodEntry; }
    public Instant getStampedAt() { return stampedAt; }
    public Integer getRating() { return rating; }
    public String getNotes() { return notes; }
}
