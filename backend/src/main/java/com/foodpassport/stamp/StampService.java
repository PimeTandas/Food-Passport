package com.foodpassport.stamp;

import com.foodpassport.common.NotFoundException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BUSINESS LOGIC layer. Controllers stay thin (HTTP in/out); the rules live here.
 * Spring injects the repository through the constructor ("dependency injection").
 */
@Service
@Transactional
public class StampService {

    private final StampRepository repository;

    public StampService(StampRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<StampResponse> findAll(String country) {
        List<Stamp> stamps = (country == null || country.isBlank())
                ? repository.findAllByOrderByDateTriedDesc()
                : repository.findByCountryIgnoreCaseOrderByDateTriedDesc(country.trim());
        return stamps.stream().map(StampResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StampResponse findById(Long id) {
        return StampResponse.from(getOrThrow(id));
    }

    public StampResponse create(StampRequest request) {
        Stamp stamp = new Stamp(
                request.dishName().trim(),
                normaliseCountry(request.country()),
                request.location(),
                request.dateTried() != null ? request.dateTried() : LocalDate.now(),
                request.rating(),
                request.notes());
        return StampResponse.from(repository.save(stamp));
    }

    public StampResponse update(Long id, StampRequest request) {
        Stamp stamp = getOrThrow(id);
        stamp.setDishName(request.dishName().trim());
        stamp.setCountry(normaliseCountry(request.country()));
        stamp.setLocation(request.location());
        if (request.dateTried() != null) {
            stamp.setDateTried(request.dateTried());
        }
        stamp.setRating(request.rating());
        stamp.setNotes(request.notes());
        // No save() needed: inside a transaction, JPA notices the changes and writes them on commit.
        return StampResponse.from(stamp);
    }

    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PassportSummary summary() {
        List<Stamp> stamps = repository.findAll();
        Map<String, Long> perCountry = stamps.stream()
                .collect(Collectors.groupingBy(Stamp::getCountry, TreeMap::new, Collectors.counting()));
        return new PassportSummary(stamps.size(), perCountry.size(), perCountry);
    }

    private Stamp getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stamp " + id + " not found"));
    }

    /** "south korea", " SOUTH  KOREA " and "South Korea" should all count as the same country. */
    static String normaliseCountry(String country) {
        return Arrays.stream(country.trim().toLowerCase().split("\\s+"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
