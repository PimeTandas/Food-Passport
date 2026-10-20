package com.foodpassport.stamp;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DATA ACCESS layer. We only declare an interface - Spring Data generates the
 * implementation (save, findById, findAll, deleteById, ...) at startup.
 * Methods like findByCountryIgnoreCase are turned into SQL from their names.
 */
public interface StampRepository extends JpaRepository<Stamp, Long> {

    List<Stamp> findByCountryIgnoreCaseOrderByDateTriedDesc(String country);

    List<Stamp> findAllByOrderByDateTriedDesc();
}
