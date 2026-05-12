package org.pinnel.pinnelapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pinnel.pinnelapi.dto.PinDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.pinnel.pinnelapi.repository.PinRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PinServiceTest {

    private static final Long CALLER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long CITY_ID = 10L;
    private static final Long PIN_ID = 100L;
    private static final Instant ORIGINAL_TIMESTAMP = Instant.parse("2026-05-01T00:00:00Z");

    @Mock
    private PinRepository pinRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private PinService pinService;

    private UserEntity caller;
    private UserEntity otherUser;
    private CityEntity city;

    @BeforeEach
    void setUp() {
        caller = UserEntity.builder().id(CALLER_ID).cognitoId("caller").username("caller").build();
        otherUser = UserEntity.builder().id(OTHER_USER_ID).cognitoId("other").username("other").build();
        city = CityEntity.builder().id(CITY_ID).name("Paris").country("France").build();
    }

    private PinEntity pin(Long id, UserEntity owner, boolean isPublic) {
        return PinEntity.builder()
                .id(id)
                .name("Eiffel Tower")
                .description("Iron lattice tower")
                .latitude(new BigDecimal("48.858400"))
                .longitude(new BigDecimal("2.294500"))
                .isPublic(isPublic)
                .city(city)
                .user(owner)
                .createdAt(ORIGINAL_TIMESTAMP)
                .updatedAt(ORIGINAL_TIMESTAMP)
                .build();
    }

    private PinDto request(Long cityId, boolean isPublic) {
        return new PinDto(
                null,
                "Eiffel Tower",
                "Iron lattice tower",
                new BigDecimal("48.858400"),
                new BigDecimal("2.294500"),
                isPublic,
                cityId,
                null, null, null
        );
    }

    @Test
    void listByCityForwardsCallerIdAndMapsResults() {
        given(pinRepository.findVisibleByCityId(CITY_ID, CALLER_ID))
                .willReturn(List.of(pin(PIN_ID, caller, false)));

        List<PinDto> result = pinService.listByCity(CITY_ID, caller);

        assertThat(result).singleElement().satisfies(p -> {
            assertThat(p.id()).isEqualTo(PIN_ID);
            assertThat(p.cityId()).isEqualTo(CITY_ID);
            assertThat(p.userId()).isEqualTo(CALLER_ID);
        });
    }

    @Test
    void getByIdReturnsCuratedPin() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, null, false)));

        PinDto result = pinService.getById(PIN_ID, caller);

        assertThat(result.id()).isEqualTo(PIN_ID);
        assertThat(result.userId()).isNull();
    }

    @Test
    void getByIdReturnsPublicPinFromAnotherUser() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, otherUser, true)));

        PinDto result = pinService.getById(PIN_ID, caller);

        assertThat(result.userId()).isEqualTo(OTHER_USER_ID);
        assertThat(result.isPublic()).isTrue();
    }

    @Test
    void getByIdReturnsOwnPrivatePin() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, caller, false)));

        PinDto result = pinService.getById(PIN_ID, caller);

        assertThat(result.userId()).isEqualTo(CALLER_ID);
    }

    @Test
    void getByIdThrows404ForPrivatePinFromAnotherUser() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, otherUser, false)));

        assertThatThrownBy(() -> pinService.getById(PIN_ID, caller))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getByIdThrows404WhenMissing() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pinService.getById(PIN_ID, caller))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void createPersistsPinOwnedByCallerWithTimestamps() {
        given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(city));
        given(pinRepository.save(any(PinEntity.class))).willAnswer(inv -> {
            PinEntity p = inv.getArgument(0);
            p.setId(PIN_ID);
            return p;
        });

        PinDto result = pinService.create(caller, request(CITY_ID, false));

        ArgumentCaptor<PinEntity> captor = ArgumentCaptor.forClass(PinEntity.class);
        verify(pinRepository).save(captor.capture());
        PinEntity saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(caller);
        assertThat(saved.getCity()).isSameAs(city);
        assertThat(saved.getName()).isEqualTo("Eiffel Tower");
        assertThat(saved.isPublic()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());

        assertThat(result.id()).isEqualTo(PIN_ID);
        assertThat(result.userId()).isEqualTo(CALLER_ID);
    }

    @Test
    void createThrows404WhenCityMissing() {
        given(cityRepository.findById(CITY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pinService.create(caller, request(CITY_ID, false)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(pinRepository, never()).save(any());
    }

    @Test
    void updateAppliesFieldsAndBumpsUpdatedAtWhenOwnerOfPrivatePin() {
        PinEntity existing = pin(PIN_ID, caller, false);
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(existing));
        given(pinRepository.save(existing)).willAnswer(inv -> inv.getArgument(0));

        PinDto update = new PinDto(
                null,
                "Renamed",
                "New description",
                new BigDecimal("48.000000"),
                new BigDecimal("2.000000"),
                false,
                CITY_ID,
                null, null, null
        );

        PinDto result = pinService.update(caller, PIN_ID, update);

        assertThat(existing.getName()).isEqualTo("Renamed");
        assertThat(existing.getDescription()).isEqualTo("New description");
        assertThat(existing.getLatitude()).isEqualByComparingTo("48.000000");
        assertThat(existing.getLongitude()).isEqualByComparingTo("2.000000");
        assertThat(existing.getUpdatedAt()).isAfter(ORIGINAL_TIMESTAMP);
        assertThat(result.name()).isEqualTo("Renamed");
    }

    @Test
    void updateLooksUpNewCityWhenCityIdChanges() {
        PinEntity existing = pin(PIN_ID, caller, false);
        CityEntity newCity = CityEntity.builder().id(99L).name("Rome").country("Italy").build();
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(existing));
        given(cityRepository.findById(99L)).willReturn(Optional.of(newCity));
        given(pinRepository.save(existing)).willAnswer(inv -> inv.getArgument(0));

        PinDto update = new PinDto(
                null, "n", "d",
                new BigDecimal("41.000000"), new BigDecimal("12.000000"),
                false, 99L,
                null, null, null
        );

        pinService.update(caller, PIN_ID, update);

        assertThat(existing.getCity()).isSameAs(newCity);
    }

    @Test
    void updateAllowsFlippingToPublic() {
        PinEntity existing = pin(PIN_ID, caller, false);
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(existing));
        given(pinRepository.save(existing)).willAnswer(inv -> inv.getArgument(0));

        pinService.update(caller, PIN_ID, request(CITY_ID, true));

        assertThat(existing.isPublic()).isTrue();
    }

    @Test
    void updateThrows403WhenNotOwner() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, otherUser, false)));

        assertThatThrownBy(() -> pinService.update(caller, PIN_ID, request(CITY_ID, false)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(pinRepository, never()).save(any());
    }

    @Test
    void updateThrows403WhenPinIsCurated() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, null, false)));

        assertThatThrownBy(() -> pinService.update(caller, PIN_ID, request(CITY_ID, false)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updateThrows403WhenPinIsAlreadyPublic() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, caller, true)));

        assertThatThrownBy(() -> pinService.update(caller, PIN_ID, request(CITY_ID, true)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updateThrows404WhenMissing() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pinService.update(caller, PIN_ID, request(CITY_ID, false)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteRemovesWhenOwnerOfPrivatePin() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, caller, false)));

        pinService.delete(caller, PIN_ID);

        verify(pinRepository).deleteById(PIN_ID);
    }

    @Test
    void deleteIsIdempotentWhenMissing() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.empty());

        pinService.delete(caller, PIN_ID);

        verify(pinRepository, never()).deleteById(any());
    }

    @Test
    void deleteThrows403WhenNotOwner() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, otherUser, false)));

        assertThatThrownBy(() -> pinService.delete(caller, PIN_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(pinRepository, never()).deleteById(any());
    }

    @Test
    void deleteThrows403WhenPinIsCurated() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, null, false)));

        assertThatThrownBy(() -> pinService.delete(caller, PIN_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void deleteThrows403WhenPinIsAlreadyPublic() {
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin(PIN_ID, caller, true)));

        assertThatThrownBy(() -> pinService.delete(caller, PIN_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
