package com.foodpassport.country;

public record CountrySummary(String code, String name, String continent, String flag) {

    public static CountrySummary from(Country c) {
        return new CountrySummary(c.getCode(), c.getName(), c.getContinent(), c.getFlag());
    }
}
