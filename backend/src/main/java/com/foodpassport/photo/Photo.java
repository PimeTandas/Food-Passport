package com.foodpassport.photo;

import com.foodpassport.stamp.FoodEntry;
import com.foodpassport.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/** Metadata about a photo. The bytes themselves live in {@link PhotoStorage} under storageKey. */
@Entity
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private FoodEntry foodEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    private User uploadedBy;

    private String storageKey;

    private String contentType;

    private Instant createdAt;

    protected Photo() {
    }

    public Photo(FoodEntry foodEntry, User uploadedBy, String storageKey, String contentType) {
        this.foodEntry = foodEntry;
        this.uploadedBy = uploadedBy;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.createdAt = Instant.now();
        foodEntry.getPhotos().add(this);
    }

    public Long getId() { return id; }
    public FoodEntry getFoodEntry() { return foodEntry; }
    public User getUploadedBy() { return uploadedBy; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public Instant getCreatedAt() { return createdAt; }
}
