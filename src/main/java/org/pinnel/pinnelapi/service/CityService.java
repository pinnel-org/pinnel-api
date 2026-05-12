package org.pinnel.pinnelapi.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.CityDto;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    /** Returns the city with the given id. Throws 404 if not found. */
    public CityDto getById(Long id) {
        return cityRepository.findById(id)
                .map(CityDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Case-insensitive prefix search by city name, ordered by population descending then name. A null or blank query returns the most populous cities first. Capped at {@code limit} results. */
    public List<CityDto> search(String search, int limit) {
        String prefix = search == null ? "" : search.trim();
        return cityRepository.searchByNamePrefix(prefix, PageRequest.ofSize(limit))
                .stream()
                .map(CityDto::from)
                .toList();
    }
}
