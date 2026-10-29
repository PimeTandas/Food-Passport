package com.foodpassport.demo;

import com.foodpassport.auth.AuthDtos.RegisterRequest;
import com.foodpassport.auth.AuthService;
import com.foodpassport.friend.FriendService;
import com.foodpassport.stamp.StampDtos.CreateStampRequest;
import com.foodpassport.stamp.StampDtos.Price;
import com.foodpassport.stamp.StampService;
import com.foodpassport.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo accounts with friends and stamps, so you can try every feature locally straight away.
 * Only runs with the "demo" profile:  mvn spring-boot:run -Dspring-boot.run.profiles=demo
 * The production deployment never sets that profile. Safe to run repeatedly: skips if already there.
 *
 * Log in as demo@foodpassport.test / demo-password (friends with sam and alex).
 */
@Component
@Profile("demo")
public class DemoData implements ApplicationRunner {

    static final String PASSWORD = "demo-password";
    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private final AuthService auth;
    private final FriendService friends;
    private final StampService stamps;
    private final UserRepository users;

    public DemoData(AuthService auth, FriendService friends, StampService stamps, UserRepository users) {
        this.auth = auth;
        this.friends = friends;
        this.stamps = stamps;
        this.users = users;
    }

    /** One transaction: if startup is interrupted halfway, nothing is saved and the next run starts over. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByUsernameIgnoreCase("demo")) {
            log.info("Demo data already present - log in as demo@foodpassport.test / {}", PASSWORD);
            return;
        }

        Long demo = register("demo");
        Long sam = register("sam");
        Long alex = register("alex");
        register("jordan"); // not a friend yet: send them a request to try that flow

        befriend(demo, sam);
        befriend(demo, alex);

        stamp(demo, "JP", "Tonkotsu ramen", "Ichiran, Shibuya", false, "1290", "JPY", 5,
                "Rich, porky broth. Would queue again.", List.of(sam));
        stamp(demo, "JP", "Okonomiyaki", null, true, null, null, 4, "First attempt at home - flipping is an art.", List.of());
        stamp(demo, "IT", "Cacio e pepe", "Roscioli, Rome", false, "16", "EUR", 5, "Three ingredients, perfect.", List.of(alex));
        stamp(demo, "MX", "Tacos al pastor", "El Vilsito, Mexico City", false, "75", "MXN", 5, null, List.of(sam, alex));
        stamp(demo, "TH", "Pad kra pao", "Street stall, Bangkok", false, "60", "THB", 4, "Ask for it 'pet mak' only if you mean it.", List.of());
        stamp(demo, "FR", "Croissant", "Du Pain et des Idées, Paris", false, "1.90", "EUR", 4, null, List.of());
        stamp(sam, "KR", "Bibimbap", "Gogung, Seoul", false, "13000", "KRW", 5, null, List.of(demo));

        log.info("Demo data created - log in as demo@foodpassport.test / {}", PASSWORD);
    }

    private Long register(String username) {
        return auth.register(new RegisterRequest(username + "@foodpassport.test", username, PASSWORD)).user().id();
    }

    private void befriend(Long from, Long to) {
        var request = friends.sendRequest(from, users.findById(to).orElseThrow().getUsername());
        friends.accept(to, request.id());
    }

    private void stamp(Long who, String country, String dish, String place, boolean homemade, String amount,
                       String currency, Integer rating, String notes, List<Long> with) {
        Price price = amount == null ? null : new Price(new BigDecimal(amount), currency);
        stamps.create(who, country, new CreateStampRequest(dish, place, homemade, price, rating, notes, with));
    }
}
