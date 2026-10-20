package com.foodpassport.country;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, String> {

    List<Country> findAllByOrderByNameAsc();
}
