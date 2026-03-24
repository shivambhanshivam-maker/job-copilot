package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.PreferredLocationDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private static final List<PreferredLocationDto> ALL_CITIES = List.of(
            new PreferredLocationDto("Berlin",     "de", "Berlin, Germany"),
            new PreferredLocationDto("Munich",     "de", "Munich, Germany"),
            new PreferredLocationDto("Hamburg",    "de", "Hamburg, Germany"),
            new PreferredLocationDto("Frankfurt",  "de", "Frankfurt, Germany"),
            new PreferredLocationDto("Cologne",    "de", "Cologne, Germany"),
            new PreferredLocationDto("Paris",      "fr", "Paris, France"),
            new PreferredLocationDto("Lyon",       "fr", "Lyon, France"),
            new PreferredLocationDto("Amsterdam",  "nl", "Amsterdam, Netherlands"),
            new PreferredLocationDto("Rotterdam",  "nl", "Rotterdam, Netherlands"),
            new PreferredLocationDto("London",     "gb", "London, United Kingdom"),
            new PreferredLocationDto("Manchester", "gb", "Manchester, United Kingdom"),
            new PreferredLocationDto("Madrid",     "es", "Madrid, Spain"),
            new PreferredLocationDto("Barcelona",  "es", "Barcelona, Spain"),
            new PreferredLocationDto("Rome",       "it", "Rome, Italy"),
            new PreferredLocationDto("Milan",      "it", "Milan, Italy"),
            new PreferredLocationDto("Vienna",     "at", "Vienna, Austria"),
            new PreferredLocationDto("Zurich",     "ch", "Zurich, Switzerland"),
            new PreferredLocationDto("Geneva",     "ch", "Geneva, Switzerland"),
            new PreferredLocationDto("Stockholm",  "se", "Stockholm, Sweden"),
            new PreferredLocationDto("Copenhagen", "dk", "Copenhagen, Denmark"),
            new PreferredLocationDto("Oslo",       "no", "Oslo, Norway"),
            new PreferredLocationDto("Helsinki",   "fi", "Helsinki, Finland"),
            new PreferredLocationDto("Brussels",   "be", "Brussels, Belgium"),
            new PreferredLocationDto("Lisbon",     "pt", "Lisbon, Portugal"),
            new PreferredLocationDto("Warsaw",     "pl", "Warsaw, Poland")
    );

    @GetMapping("/search")
    public List<PreferredLocationDto> search(@RequestParam String query) {
        if (query == null || query.isBlank()) return List.of();

        String q = query.toLowerCase().trim();
        return ALL_CITIES.stream()
                .filter(city -> city.getDisplayName().toLowerCase().contains(q))
                .toList();
    }
}