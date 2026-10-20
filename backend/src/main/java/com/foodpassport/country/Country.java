package com.foodpassport.country;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A country page in the passport. All ~250 are preloaded by db/migration/V2__seed_countries.sql. */
@Entity
public class Country {

    /** ISO 3166-1 alpha-2 code, e.g. "JP". */
    @Id
    private String code;

    private String name;

    private String continent;

    /** Emoji flag, e.g. 🇯🇵. */
    private String flag;

    protected Country() {
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getContinent() { return continent; }
    public String getFlag() { return flag; }
}
