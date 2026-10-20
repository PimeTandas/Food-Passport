package com.foodpassport.stamp;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StampRepository extends JpaRepository<Stamp, Long> {

    Optional<Stamp> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndFoodEntryId(Long ownerId, Long foodEntryId);

    /** Everything on one of my country pages, newest first. */
    @Query("""
            select s from Stamp s join fetch s.foodEntry e
            where s.owner.id = :ownerId and e.country.code = :countryCode
            order by s.stampedAt desc, s.id desc
            """)
    List<Stamp> findPage(@Param("ownerId") Long ownerId, @Param("countryCode") String countryCode);

    /** How many stamps I have per country: rows of [countryCode, count]. */
    @Query("""
            select s.foodEntry.country.code, count(s) from Stamp s
            where s.owner.id = :ownerId
            group by s.foodEntry.country.code
            """)
    List<Object[]> countPerCountry(@Param("ownerId") Long ownerId);
}
