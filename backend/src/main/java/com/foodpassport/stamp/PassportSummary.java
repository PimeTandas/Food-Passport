package com.foodpassport.stamp;

import java.util.Map;

/** The front page of your passport: how much of the world you've eaten. */
public record PassportSummary(
        long totalStamps,
        long countriesVisited,
        Map<String, Long> stampsPerCountry) {
}
