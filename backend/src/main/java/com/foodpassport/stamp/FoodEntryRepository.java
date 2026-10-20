package com.foodpassport.stamp;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodEntryRepository extends JpaRepository<FoodEntry, Long> {
}
