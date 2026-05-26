package org.pinnel.pinnelapi.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.CityDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Value("${pinnel.cdn.base-url}")
    private String cdnBaseUrl;

    @Value("${pinnel.cdn.cover-path-template}")
    private String coverPathTemplate;

    @Value("${pinnel.cdn.cover-count}")
    private int coverCount;

    /** Returns the city with the given id. Throws 404 if not found. */
    public CityDto getById(Long id) {
        return cityRepository.findById(id)
                .map(CityDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Case-insensitive prefix search by city name, ordered by population descending then name. A blank query returns the most populous cities first. Capped at {@code limit} results. */
    public List<CityDto> search(String search, int limit) {
        return cityRepository.searchByNamePrefix(search.trim(), PageRequest.ofSize(limit))
                .stream()
                .map(CityDto::from)
                .toList();
    }

    /** Builds a CloudFront URL for one of the city's cover images, picked at random in [1, cover-count]. City and country names must be ASCII English; non-ASCII characters would be stripped to hyphens and produce a broken URL. */
    public String buildCoverUrl(CityEntity city) {
        int n = ThreadLocalRandom.current().nextInt(1, coverCount + 1);
        String path = coverPathTemplate
                .replace("{country}", slugify(city.getCountry()))
                .replace("{city}", slugify(city.getName()))
                .replace("{n}", String.valueOf(n));
        return cdnBaseUrl + path;
    }

    private static String slugify(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
