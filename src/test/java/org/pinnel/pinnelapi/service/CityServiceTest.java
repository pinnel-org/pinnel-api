package org.pinnel.pinnelapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pinnel.pinnelapi.dto.CityDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    private static final int SEARCH_LIMIT = 20;
    private static final Long PARIS_ID = 1L;
    private static final Long MISSING_ID = 99L;
    private static final String PARIS_NAME = "Paris";
    private static final String PARIS_COUNTRY = "France";
    private static final int PARIS_POPULATION = 2_140_000;
    private static final String PARIS_LATITUDE = "48.856600";
    private static final String PARIS_LONGITUDE = "2.352200";

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityService cityService;

    private final CityEntity paris = CityEntity.builder()
            .id(PARIS_ID)
            .name(PARIS_NAME)
            .country(PARIS_COUNTRY)
            .latitude(new BigDecimal(PARIS_LATITUDE))
            .longitude(new BigDecimal(PARIS_LONGITUDE))
            .population(PARIS_POPULATION)
            .build();

    @Test
    void getByIdReturnsDtoWhenFound() {
        given(cityRepository.findById(PARIS_ID)).willReturn(Optional.of(paris));

        CityDto result = cityService.getById(PARIS_ID);

        assertThat(result.id()).isEqualTo(PARIS_ID);
        assertThat(result.name()).isEqualTo(PARIS_NAME);
        assertThat(result.country()).isEqualTo(PARIS_COUNTRY);
        assertThat(result.population()).isEqualTo(PARIS_POPULATION);
        assertThat(result.latitude()).isEqualByComparingTo(PARIS_LATITUDE);
        assertThat(result.longitude()).isEqualByComparingTo(PARIS_LONGITUDE);
    }

    @Test
    void getByIdThrows404WhenMissing() {
        given(cityRepository.findById(MISSING_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.getById(MISSING_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void searchTrimsPrefixAndForwardsLimit() {
        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(cityRepository.searchByNamePrefix(prefixCaptor.capture(), pageableCaptor.capture()))
                .willReturn(List.of(paris));

        List<CityDto> result = cityService.search("  Par  ", SEARCH_LIMIT);

        assertThat(prefixCaptor.getValue()).isEqualTo("Par");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(SEARCH_LIMIT);
        assertThat(result).singleElement().satisfies(c -> assertThat(c.name()).isEqualTo(PARIS_NAME));
    }

    @Test
    void searchWithBlankUsesEmptyPrefix() {
        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        given(cityRepository.searchByNamePrefix(prefixCaptor.capture(), any(Pageable.class)))
                .willReturn(List.of(paris));

        cityService.search("", SEARCH_LIMIT);
        cityService.search("   ", SEARCH_LIMIT);

        assertThat(prefixCaptor.getAllValues()).containsExactly("", "");
    }
}
