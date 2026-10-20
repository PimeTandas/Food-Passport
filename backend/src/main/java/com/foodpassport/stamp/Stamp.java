package com.foodpassport.stamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

/**
 * A "stamp" in your food passport: one dish you tried, and where it comes from.
 * This class is the DATA layer - JPA turns it into a "stamp" table.
 */
@Entity
public class Stamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String dishName;

    /** The dish's country of origin, e.g. "Japan". */
    @Column(nullable = false)
    private String country;

    /** Where you ate it (optional), e.g. "Bob's Ramen, Dublin". */
    private String location;

    @Column(nullable = false)
    private LocalDate dateTried;

    /** 1-5 stars. */
    private Integer rating;

    @Column(length = 1000)
    private String notes;

    /** JPA needs a no-args constructor to build objects from database rows. */
    protected Stamp() {
    }

    public Stamp(String dishName, String country, String location, LocalDate dateTried, Integer rating, String notes) {
        this.dishName = dishName;
        this.country = country;
        this.location = location;
        this.dateTried = dateTried;
        this.rating = rating;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public String getDishName() { return dishName; }
    public String getCountry() { return country; }
    public String getLocation() { return location; }
    public LocalDate getDateTried() { return dateTried; }
    public Integer getRating() { return rating; }
    public String getNotes() { return notes; }

    public void setDishName(String dishName) { this.dishName = dishName; }
    public void setCountry(String country) { this.country = country; }
    public void setLocation(String location) { this.location = location; }
    public void setDateTried(LocalDate dateTried) { this.dateTried = dateTried; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setNotes(String notes) { this.notes = notes; }
}
