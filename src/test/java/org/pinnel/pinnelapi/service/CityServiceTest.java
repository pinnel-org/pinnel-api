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

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityService cityService;

    private final CityEntity paris = CityEntity.builder()
            .id(1L)
            .name("Paris")
            .country("France")
            .latitude(new BigDecimal("48.856600"))
            .longitude(new BigDecimal("2.352200"))
            .population(2_140_000)
            .build();

    @Test
    void getByIdReturnsDtoWhenFound() {
        long id = 1L;
        given(cityRepository.findById(id)).willReturn(Optional.of(paris));

        CityDto result = cityService.getById(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Paris");
        assertThat(result.country()).isEqualTo("France");
        assertThat(result.population()).isEqualTo(2_140_000);
        assertThat(result.latitude()).isEqualByComparingTo("48.856600");
        assertThat(result.longitude()).isEqualByComparingTo("2.352200");
    }

    @Test
    void getByIdThrows404WhenMissing() {
        long missingId = 99L;
        given(cityRepository.findById(missingId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.getById(missingId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void searchTrimsPrefixAndForwardsLimit() {
        int limit = 20;
        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(cityRepository.searchByNamePrefix(prefixCaptor.capture(), pageableCaptor.capture()))
                .willReturn(List.of(paris));

        List<CityDto> result = cityService.search("  Par  ", limit);

        assertThat(prefixCaptor.getValue()).isEqualTo("Par");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(limit);
        assertThat(result).singleElement().satisfies(c -> assertThat(c.name()).isEqualTo("Paris"));
    }

    @Test
    void searchWithBlankUsesEmptyPrefix() {
        int limit = 20;
        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        given(cityRepository.searchByNamePrefix(prefixCaptor.capture(), any(Pageable.class)))
                .willReturn(List.of(paris));

        cityService.search("", limit);
        cityService.search("   ", limit);

        assertThat(prefixCaptor.getAllValues()).containsExactly("", "");
    }
}
