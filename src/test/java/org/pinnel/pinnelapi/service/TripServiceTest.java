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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pinnel.pinnelapi.dto.TripDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final Long CALLER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long TRIP_ID = 100L;
    private static final Instant ORIGINAL_TIMESTAMP = Instant.parse("2026-05-01T00:00:00Z");

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripService tripService;

    private UserEntity caller;
    private UserEntity otherUser;

    @BeforeEach
    void setUp() {
        caller = UserEntity.builder().id(CALLER_ID).cognitoId("caller").username("caller").build();
        otherUser = UserEntity.builder().id(OTHER_USER_ID).cognitoId("other").username("other").build();
    }

    private TripEntity trip(Long id, UserEntity owner) {
        return TripEntity.builder()
                .id(id)
                .name("Summer in Italy")
                .budget(new BigDecimal("1500.00"))
                .user(owner)
                .createdAt(ORIGINAL_TIMESTAMP)
                .updatedAt(ORIGINAL_TIMESTAMP)
                .cities(Set.of(CityEntity.builder().id(10L).build()))
                .pins(Set.of(PinEntity.builder().id(20L).build()))
                .build();
    }

    private TripDto request(String name, BigDecimal budget) {
        return new TripDto(null, name, budget, null, null, null, null, null);
    }

    @Test
    void listMineReturnsCallerTripsWithCityAndPinIds() {
        given(tripRepository.findByUserId(CALLER_ID)).willReturn(List.of(trip(TRIP_ID, caller)));

        List<TripDto> result = tripService.listMine(caller);

        assertThat(result).singleElement().satisfies(t -> {
            assertThat(t.id()).isEqualTo(TRIP_ID);
            assertThat(t.userId()).isEqualTo(CALLER_ID);
            assertThat(t.cityIds()).containsExactly(10L);
            assertThat(t.pinIds()).containsExactly(20L);
        });
    }

    @Test
    void getByIdReturnsOwnTrip() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip(TRIP_ID, caller)));

        TripDto result = tripService.getById(caller, TRIP_ID);

        assertThat(result.id()).isEqualTo(TRIP_ID);
        assertThat(result.cityIds()).containsExactly(10L);
        assertThat(result.pinIds()).containsExactly(20L);
    }

    @Test
    void getByIdThrows404WhenMissing() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getById(caller, TRIP_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getByIdThrows404WhenOwnedByAnotherUser() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip(TRIP_ID, otherUser)));

        assertThatThrownBy(() -> tripService.getById(caller, TRIP_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void createPersistsTripOwnedByCallerWithTimestamps() {
        given(tripRepository.save(any(TripEntity.class))).willAnswer(inv -> {
            TripEntity t = inv.getArgument(0);
            t.setId(TRIP_ID);
            return t;
        });

        TripDto result = tripService.create(caller, request("New Trip", new BigDecimal("500.00")));

        ArgumentCaptor<TripEntity> captor = ArgumentCaptor.forClass(TripEntity.class);
        verify(tripRepository).save(captor.capture());
        TripEntity saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(caller);
        assertThat(saved.getName()).isEqualTo("New Trip");
        assertThat(saved.getBudget()).isEqualByComparingTo("500.00");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(saved.getCities()).isEmpty();
        assertThat(saved.getPins()).isEmpty();

        assertThat(result.id()).isEqualTo(TRIP_ID);
        assertThat(result.userId()).isEqualTo(CALLER_ID);
    }

    @Test
    void createAcceptsNullBudget() {
        given(tripRepository.save(any(TripEntity.class))).willAnswer(inv -> inv.getArgument(0));

        tripService.create(caller, request("No budget", null));

        ArgumentCaptor<TripEntity> captor = ArgumentCaptor.forClass(TripEntity.class);
        verify(tripRepository).save(captor.capture());
        assertThat(captor.getValue().getBudget()).isNull();
    }

    @Test
    void updateAppliesFieldsAndBumpsUpdatedAtWhenOwner() {
        TripEntity existing = trip(TRIP_ID, caller);
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(existing));
        given(tripRepository.save(existing)).willAnswer(inv -> inv.getArgument(0));

        TripDto result = tripService.update(caller, TRIP_ID, request("Renamed", new BigDecimal("2000.00")));

        assertThat(existing.getName()).isEqualTo("Renamed");
        assertThat(existing.getBudget()).isEqualByComparingTo("2000.00");
        assertThat(existing.getUpdatedAt()).isAfter(ORIGINAL_TIMESTAMP);
        assertThat(result.name()).isEqualTo("Renamed");
    }

    @Test
    void updateThrows404WhenMissing() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.update(caller, TRIP_ID, request("x", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripRepository, never()).save(any());
    }

    @Test
    void updateThrows404WhenOwnedByAnotherUser() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip(TRIP_ID, otherUser)));

        assertThatThrownBy(() -> tripService.update(caller, TRIP_ID, request("x", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripRepository, never()).save(any());
    }

    @Test
    void deleteRemovesWhenOwner() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip(TRIP_ID, caller)));

        tripService.delete(caller, TRIP_ID);

        verify(tripRepository).deleteById(TRIP_ID);
    }

    @Test
    void deleteIsIdempotentWhenMissing() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.empty());

        tripService.delete(caller, TRIP_ID);

        verify(tripRepository, never()).deleteById(any());
    }

    @Test
    void deleteThrows404WhenOwnedByAnotherUser() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip(TRIP_ID, otherUser)));

        assertThatThrownBy(() -> tripService.delete(caller, TRIP_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripRepository, never()).deleteById(any());
    }
}
