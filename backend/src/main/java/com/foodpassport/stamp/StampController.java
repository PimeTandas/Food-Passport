package com.foodpassport.stamp;

import com.foodpassport.auth.CurrentUser;
import com.foodpassport.stamp.StampDtos.CompanionsRequest;
import com.foodpassport.stamp.StampDtos.CountryListItem;
import com.foodpassport.stamp.StampDtos.CountryPage;
import com.foodpassport.stamp.StampDtos.CreateStampRequest;
import com.foodpassport.stamp.StampDtos.PassportSummary;
import com.foodpassport.stamp.StampDtos.ReviewRequest;
import com.foodpassport.stamp.StampDtos.StampResponse;
import com.foodpassport.stamp.StampDtos.UpdateEntryRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Everything is "mine": the logged-in user's passport, pages and stamps. */
@RestController
@RequestMapping("/api")
public class StampController {

    private final StampService service;

    public StampController(StampService service) {
        this.service = service;
    }

    @GetMapping("/passport")
    public PassportSummary passport(@AuthenticationPrincipal Jwt jwt) {
        return service.passport(CurrentUser.id(jwt));
    }

    @GetMapping("/countries")
    public List<CountryListItem> countries(@AuthenticationPrincipal Jwt jwt) {
        return service.countries(CurrentUser.id(jwt));
    }

    @GetMapping("/countries/{code}")
    public CountryPage page(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return service.page(CurrentUser.id(jwt), code);
    }

    @PostMapping("/countries/{code}/stamps")
    @ResponseStatus(HttpStatus.CREATED)
    public StampResponse stamp(@AuthenticationPrincipal Jwt jwt, @PathVariable String code,
                               @Valid @RequestBody CreateStampRequest request) {
        return service.create(CurrentUser.id(jwt), code, request);
    }

    @GetMapping("/stamps/{id}")
    public StampResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/stamps/{id}/review")
    public StampResponse review(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @Valid @RequestBody ReviewRequest request) {
        return service.review(CurrentUser.id(jwt), id, request);
    }

    @PutMapping("/stamps/{id}/entry")
    public StampResponse updateEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @Valid @RequestBody UpdateEntryRequest request) {
        return service.updateEntry(CurrentUser.id(jwt), id, request);
    }

    @PutMapping("/stamps/{id}/companions")
    public StampResponse companions(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                    @Valid @RequestBody CompanionsRequest request) {
        return service.setCompanions(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/stamps/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }
}
