package com.foodpassport.stamp;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * WEB layer: maps HTTP requests to service calls. Objects returned here are turned into JSON.
 */
@RestController
@RequestMapping("/api")
public class StampController {

    private final StampService service;

    public StampController(StampService service) {
        this.service = service;
    }

    @GetMapping("/stamps")
    public List<StampResponse> list(@RequestParam(required = false) String country) {
        return service.findAll(country);
    }

    @GetMapping("/stamps/{id}")
    public StampResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/stamps")
    @ResponseStatus(HttpStatus.CREATED)
    public StampResponse create(@Valid @RequestBody StampRequest request) {
        return service.create(request);
    }

    @PutMapping("/stamps/{id}")
    public StampResponse update(@PathVariable Long id, @Valid @RequestBody StampRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/stamps/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/passport")
    public PassportSummary passport() {
        return service.summary();
    }
}
