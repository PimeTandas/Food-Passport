package com.foodpassport.stamp;

import com.foodpassport.country.Country;
import com.foodpassport.photo.Photo;
import com.foodpassport.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The shared food event: what was eaten, where, and what it cost.
 * Everyone who was there has their own {@link Stamp} pointing at this one entry.
 */
@Entity
public class FoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Country country;

    /** Who logged it - the only person allowed to change who is tagged. */
    @ManyToOne(fetch = FetchType.LAZY)
    private User createdBy;

    private String dishName;
    private String placeName;
    private boolean homemade;
    private BigDecimal priceAmount;
    private String priceCurrency;
    private Instant createdAt;

    @OneToMany(mappedBy = "foodEntry")
    @OrderBy("stampedAt asc, id asc")
    private List<Stamp> stamps = new ArrayList<>();

    @OneToMany(mappedBy = "foodEntry")
    @OrderBy("createdAt asc, id asc")
    private List<Photo> photos = new ArrayList<>();

    protected FoodEntry() {
    }

    public FoodEntry(Country country, User createdBy) {
        this.country = country;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public void updateDetails(String dishName, String placeName, boolean homemade,
                              BigDecimal priceAmount, String priceCurrency) {
        this.dishName = dishName;
        this.placeName = placeName;
        this.homemade = homemade;
        this.priceAmount = priceAmount;
        this.priceCurrency = priceCurrency;
    }

    public boolean isCreatedBy(Long userId) {
        return createdBy != null && createdBy.getId().equals(userId);
    }

    public Long getId() { return id; }
    public Country getCountry() { return country; }
    public User getCreatedBy() { return createdBy; }
    public String getDishName() { return dishName; }
    public String getPlaceName() { return placeName; }
    public boolean isHomemade() { return homemade; }
    public BigDecimal getPriceAmount() { return priceAmount; }
    public String getPriceCurrency() { return priceCurrency; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Stamp> getStamps() { return stamps; }
    public List<Photo> getPhotos() { return photos; }
}
