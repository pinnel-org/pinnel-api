package org.pinnel.pinnelapi.controller;

import java.util.List;
import org.pinnel.pinnelapi.dto.CityDto;
import org.pinnel.pinnelapi.service.CityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;
    private final int searchLimit;

    public CityController(CityService cityService,
                          @Value("${pinnel.cities.search-limit}") int searchLimit) {
        this.cityService = cityService;
        this.searchLimit = searchLimit;
    }

    /** GET /api/cities?search=... — autocomplete-style prefix search by city name, ordered by population descending. The result cap is configurable via {@code pinnel.cities.search-limit}. Public — does not require authentication. */
    @GetMapping
    public List<CityDto> search(@RequestParam(defaultValue = "") String search) {
        return cityService.search(search, searchLimit);
    }

    /** GET /api/cities/{id} — returns the city's details. 404 if not found. Public — does not require authentication. */
    @GetMapping("/{id}")
    public CityDto getById(@PathVariable Long id) {
        return cityService.getById(id);
    }
}
