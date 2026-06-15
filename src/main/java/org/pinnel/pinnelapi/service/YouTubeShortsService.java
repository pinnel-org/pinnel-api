package org.pinnel.pinnelapi.service;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.pinnel.pinnelapi.dto.CityDto;
import org.pinnel.pinnelapi.dto.PinDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Searches YouTube for short videos about a pin's place so the frontend can embed them
 * ("feel the vibe" feed). The YouTube API key stays backend-only — the client only ever
 * receives video ids. Pre-MVP: no caching, every call hits YouTube live (~100 quota units).
 */
@Service
@Slf4j
public class YouTubeShortsService {

    private final PinService pinService;
    private final CityService cityService;
    private final String apiKey;
    private final int maxResults;
    private final RestClient restClient;

    public YouTubeShortsService(PinService pinService,
                                CityService cityService,
                                @Value("${pinnel.youtube.api-key}") String apiKey,
                                @Value("${pinnel.youtube.base-url}") String baseUrl,
                                @Value("${pinnel.youtube.max-results}") int maxResults) {
        this.pinService = pinService;
        this.cityService = cityService;
        this.apiKey = apiKey;
        this.maxResults = maxResults;
        this.restClient = RestClient.create(baseUrl);
    }

    /**
     * Returns YouTube video ids for short videos about the given pin's place. Applies the same
     * visibility rules as {@code GET /api/pins/{id}} — throws 404 if the pin is not visible to the
     * caller. Throws 404 if YouTube returns no matching videos, and 502 if the upstream call fails
     * (quota exhausted, 403, 5xx, malformed response).
     */
    public List<String> findShortsForPin(Long pinId, UserEntity caller) {
        PinDto pin = pinService.getById(pinId, caller);
        CityDto city = cityService.getById(pin.cityId());
        String query = "%s %s %s travel".formatted(pin.name(), city.name(), city.country());

        List<String> videoIds = extractVideoIds(search(pinId, query));
        if (videoIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No shorts found for this pin");
        }
        return videoIds;
    }

    private SearchResponse search(Long pinId, String query) {
        try {
            return restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("videoDuration", "short")
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("maxResults", maxResults)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(SearchResponse.class);
        } catch (Exception e) {
            log.error("YouTube search.list failed for pin {} (query=\"{}\"). "
                    + "If this is a 403, the daily quota may be exhausted (each search costs 100 units).",
                    pinId, query, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch shorts from YouTube");
        }
    }

    private static List<String> extractVideoIds(SearchResponse response) {
        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items().stream()
                .map(SearchResponse.Item::id)
                .filter(Objects::nonNull)
                .map(SearchResponse.Id::videoId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
    }

    /**
     * Subset of the YouTube {@code search.list} response we care about. Unknown fields (snippet,
     * pageInfo, …) are ignored by Jackson's default lenient deserialization.
     */
    private record SearchResponse(List<Item> items) {
        private record Item(Id id) {}

        private record Id(String videoId) {}
    }
}
