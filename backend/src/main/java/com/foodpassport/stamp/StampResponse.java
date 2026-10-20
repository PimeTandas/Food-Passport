package com.foodpassport.stamp;

import java.time.LocalDate;

/** What we SEND BACK to clients. */
public record StampResponse(
        Long id,
        String dishName,
        String country,
        String location,
        LocalDate dateTried,
        Integer rating,
        String notes) {

    static StampResponse from(Stamp stamp) {
        return new StampResponse(
                stamp.getId(),
                stamp.getDishName(),
                stamp.getCountry(),
                stamp.getLocation(),
                stamp.getDateTried(),
                stamp.getRating(),
                stamp.getNotes());
    }
}
