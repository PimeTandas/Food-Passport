package com.foodpassport.stamp;

import com.foodpassport.country.CountrySummary;
import com.foodpassport.photo.PhotoResponse;
import com.foodpassport.user.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** The JSON shapes going in and out of the stamp endpoints. */
public final class StampDtos {

    private StampDtos() {
    }

    // ---------- requests ----------

    public record Price(
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "ISO 4217 code, e.g. EUR") String currency) {
    }

    /** Stamp a country page: the food entry, your own review, and which friends were there. */
    public record CreateStampRequest(
            @NotBlank @Size(max = 200) String dishName,
            @Size(max = 200) String placeName,
            Boolean homemade,
            @Valid Price price,
            @Min(1) @Max(5) Integer rating,
            @Size(max = 2000) String notes,
            List<Long> withFriendIds) {
    }

    /** The shared details of the food event - any participant can change these. */
    public record UpdateEntryRequest(
            @NotBlank @Size(max = 200) String dishName,
            @Size(max = 200) String placeName,
            Boolean homemade,
            @Valid Price price) {
    }

    /** Your personal take - only on your own stamp. */
    public record ReviewRequest(@Min(1) @Max(5) Integer rating, @Size(max = 2000) String notes) {
    }

    /** The complete list of friends who were there (replaces the old list). Entry creator only. */
    public record CompanionsRequest(@NotNull List<Long> friendIds) {
    }

    // ---------- responses ----------

    public record EntryResponse(
            Long id,
            String dishName,
            String placeName,
            boolean homemade,
            Price price,
            UserSummary createdBy,
            List<UserSummary> with,
            List<PhotoResponse> photos) {
    }

    public record StampResponse(
            Long id,
            Instant stampedAt,
            Integer rating,
            String notes,
            CountrySummary country,
            EntryResponse entry) {
    }

    public record CountryListItem(String code, String name, String continent, String flag, long stampCount) {
    }

    public record CountryPage(CountrySummary country, List<StampResponse> stamps) {
    }

    public record PassportSummary(
            long totalStamps,
            long countriesStamped,
            long totalCountries,
            Map<String, Long> countriesStampedPerContinent) {
    }
}
