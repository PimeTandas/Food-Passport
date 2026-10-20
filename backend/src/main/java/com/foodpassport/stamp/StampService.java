package com.foodpassport.stamp;

import com.foodpassport.common.ApiException;
import com.foodpassport.country.Country;
import com.foodpassport.country.CountryRepository;
import com.foodpassport.country.CountrySummary;
import com.foodpassport.friend.FriendService;
import com.foodpassport.photo.PhotoResponse;
import com.foodpassport.photo.PhotoService;
import com.foodpassport.stamp.StampDtos.CompanionsRequest;
import com.foodpassport.stamp.StampDtos.CountryListItem;
import com.foodpassport.stamp.StampDtos.CountryPage;
import com.foodpassport.stamp.StampDtos.CreateStampRequest;
import com.foodpassport.stamp.StampDtos.EntryResponse;
import com.foodpassport.stamp.StampDtos.PassportSummary;
import com.foodpassport.stamp.StampDtos.Price;
import com.foodpassport.stamp.StampDtos.ReviewRequest;
import com.foodpassport.stamp.StampDtos.StampResponse;
import com.foodpassport.stamp.StampDtos.UpdateEntryRequest;
import com.foodpassport.user.User;
import com.foodpassport.user.UserRepository;
import com.foodpassport.user.UserSummary;
import java.time.Instant;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The heart of the app. Rules:
 *  - Stamping a country page creates ONE food entry plus a stamp for you and for each tagged friend.
 *  - Stamps are dated automatically.
 *  - Shared details (dish, place, price...) can be edited by anyone who has a stamp on the entry.
 *  - Rating and notes are per stamp, so per person.
 *  - Only the entry's creator changes who was tagged.
 *  - When the last stamp on an entry is removed, the entry and its photos go too.
 */
@Service
@Transactional
public class StampService {

    private final StampRepository stamps;
    private final FoodEntryRepository entries;
    private final CountryRepository countries;
    private final UserRepository users;
    private final FriendService friends;
    private final PhotoService photos;

    public StampService(StampRepository stamps, FoodEntryRepository entries, CountryRepository countries,
                        UserRepository users, FriendService friends, PhotoService photos) {
        this.stamps = stamps;
        this.entries = entries;
        this.countries = countries;
        this.users = users;
        this.friends = friends;
        this.photos = photos;
    }

    public StampResponse create(Long meId, String countryCode, CreateStampRequest request) {
        Country country = findCountry(countryCode);
        Set<Long> friendIds = cleanFriendIds(meId, request.withFriendIds());
        friends.requireAllFriends(meId, friendIds);

        FoodEntry entry = new FoodEntry(country, users.getReferenceById(meId));
        applyDetails(entry, request.dishName(), request.placeName(), request.homemade(), request.price());
        entries.save(entry);

        Instant now = Instant.now();
        Stamp mine = new Stamp(users.getReferenceById(meId), entry, now);
        mine.review(request.rating(), blankToNull(request.notes()));
        stamps.save(mine);
        // Tagged friends get the same stamp on their own page straight away (they can remove it).
        for (Long friendId : friendIds) {
            stamps.save(new Stamp(users.getReferenceById(friendId), entry, now));
        }
        return toResponse(mine, meId);
    }

    @Transactional(readOnly = true)
    public StampResponse get(Long meId, Long stampId) {
        return toResponse(findMine(meId, stampId), meId);
    }

    public StampResponse review(Long meId, Long stampId, ReviewRequest request) {
        Stamp stamp = findMine(meId, stampId);
        stamp.review(request.rating(), blankToNull(request.notes()));
        return toResponse(stamp, meId);
    }

    public StampResponse updateEntry(Long meId, Long stampId, UpdateEntryRequest request) {
        Stamp stamp = findMine(meId, stampId);
        applyDetails(stamp.getFoodEntry(), request.dishName(), request.placeName(), request.homemade(), request.price());
        return toResponse(stamp, meId);
    }

    public StampResponse setCompanions(Long meId, Long stampId, CompanionsRequest request) {
        Stamp stamp = findMine(meId, stampId);
        FoodEntry entry = stamp.getFoodEntry();
        if (!entry.isCreatedBy(meId)) {
            throw ApiException.forbidden("Only the person who logged this entry can change who was there");
        }

        Set<Long> wanted = cleanFriendIds(meId, request.friendIds());
        Set<Long> current = entry.getStamps().stream()
                .map(s -> s.getOwner().getId())
                .filter(id -> !id.equals(meId))
                .collect(Collectors.toSet());

        Set<Long> toAdd = new LinkedHashSet<>(wanted);
        toAdd.removeAll(current);
        friends.requireAllFriends(meId, toAdd); // people already tagged can stay even if you've since unfriended

        entry.getStamps().removeIf(s -> {
            boolean remove = !s.getOwner().getId().equals(meId) && !wanted.contains(s.getOwner().getId());
            if (remove) {
                stamps.delete(s);
            }
            return remove;
        });
        for (Long friendId : toAdd) {
            stamps.save(new Stamp(users.getReferenceById(friendId), entry, stamp.getStampedAt()));
        }
        return toResponse(stamp, meId);
    }

    /** Remove my stamp. Tagged friends use this to "untag" themselves. */
    public void delete(Long meId, Long stampId) {
        Stamp stamp = findMine(meId, stampId);
        FoodEntry entry = stamp.getFoodEntry();
        entry.getStamps().remove(stamp);
        stamps.delete(stamp);
        if (entry.getStamps().isEmpty()) {
            photos.deleteAllFor(entry);
            entries.delete(entry);
        }
    }

    @Transactional(readOnly = true)
    public List<CountryListItem> countries(Long meId) {
        Map<String, Long> counts = stampCounts(meId);
        return countries.findAllByOrderByNameAsc().stream()
                .map(c -> new CountryListItem(c.getCode(), c.getName(), c.getContinent(), c.getFlag(),
                        counts.getOrDefault(c.getCode(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CountryPage page(Long meId, String countryCode) {
        Country country = findCountry(countryCode);
        List<StampResponse> page = stamps.findPage(meId, country.getCode()).stream()
                .map(s -> toResponse(s, meId))
                .toList();
        return new CountryPage(CountrySummary.from(country), page);
    }

    @Transactional(readOnly = true)
    public PassportSummary passport(Long meId) {
        Map<String, Long> counts = stampCounts(meId);
        List<Country> all = countries.findAll();
        Map<String, Long> perContinent = all.stream()
                .filter(c -> counts.containsKey(c.getCode()))
                .collect(Collectors.groupingBy(Country::getContinent, TreeMap::new, Collectors.counting()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new PassportSummary(total, counts.size(), all.size(), perContinent);
    }

    // ---------- helpers ----------

    private Stamp findMine(Long meId, Long stampId) {
        // Someone else's stamp gives the same 404 as a missing one - don't reveal what exists.
        return stamps.findByIdAndOwnerId(stampId, meId)
                .orElseThrow(() -> ApiException.notFound("Stamp " + stampId + " not found"));
    }

    private Country findCountry(String code) {
        return countries.findById(code.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("No country with code " + code));
    }

    private Map<String, Long> stampCounts(Long meId) {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : stamps.countPerCountry(meId)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }

    private static Set<Long> cleanFriendIds(Long meId, List<Long> ids) {
        if (ids == null) {
            return Set.of();
        }
        if (ids.contains(meId)) {
            throw ApiException.badRequest("You don't need to tag yourself");
        }
        return new LinkedHashSet<>(ids);
    }

    private static void applyDetails(FoodEntry entry, String dishName, String placeName, Boolean homemade, Price price) {
        String currency = null;
        if (price != null) {
            currency = price.currency().toUpperCase();
            try {
                Currency.getInstance(currency);
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("Unknown currency " + price.currency());
            }
        }
        entry.updateDetails(dishName.trim(), blankToNull(placeName), Boolean.TRUE.equals(homemade),
                price == null ? null : price.amount(), currency);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static StampResponse toResponse(Stamp stamp, Long meId) {
        FoodEntry e = stamp.getFoodEntry();
        List<UserSummary> with = e.getStamps().stream()
                .map(Stamp::getOwner)
                .filter(u -> !u.getId().equals(meId))
                .map(UserSummary::from)
                .toList();
        Price price = e.getPriceAmount() == null ? null : new Price(e.getPriceAmount(), e.getPriceCurrency());
        EntryResponse entry = new EntryResponse(e.getId(), e.getDishName(), e.getPlaceName(), e.isHomemade(), price,
                UserSummary.from(e.getCreatedBy()), with, e.getPhotos().stream().map(PhotoResponse::from).toList());
        return new StampResponse(stamp.getId(), stamp.getStampedAt(), stamp.getRating(), stamp.getNotes(),
                CountrySummary.from(e.getCountry()), entry);
    }
}
