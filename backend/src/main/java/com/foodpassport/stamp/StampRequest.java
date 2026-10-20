package com.foodpassport.stamp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * What a client SENDS us to create or update a stamp.
 * Kept separate from the entity so clients can't set fields like "id" themselves.
 */
public record StampRequest(
        @NotBlank @Size(max = 200) String dishName,
        @NotBlank @Size(max = 100) String country,
        @Size(max = 200) String location,
        @PastOrPresent LocalDate dateTried,
        @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String notes) {
}
