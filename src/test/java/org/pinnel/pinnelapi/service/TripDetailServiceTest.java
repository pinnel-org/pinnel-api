package org.pinnel.pinnelapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pinnel.pinnelapi.dto.TripDetailDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.TripDetailEntity;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.pinnel.pinnelapi.repository.TripDetailRepository;
import org.pinnel.pinnelapi.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TripDetailServiceTest {

    private static final Long CALLER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long TRIP_ID = 10L;
    private static final Long CITY_ID = 20L;
    private static final Long DETAIL_ID = 100L;
    private static final LocalDate VISIT_DATE = LocalDate.of(2026, 7, 15);

    @Mock private TripRepository tripRepository;
    @Mock private CityRepository cityRepository;
    @Mock private TripDetailRepository tripDetailRepository;

    @InjectMocks private TripDetailService tripDetailService;

    private UserEntity caller;
    private UserEntity otherUser;
    private TripEntity trip;
    private CityEntity city;

    @BeforeEach
    void setUp() {
        caller = UserEntity.builder().id(CALLER_ID).cognitoId("caller").username("caller").build();
        otherUser = UserEntity.builder().id(OTHER_USER_ID).cognitoId("other").username("other").build();
        trip = TripEntity.builder().id(TRIP_ID).name("Rome").user(caller).build();
        city = CityEntity.builder().id(CITY_ID).name("Rome").country("Italy").build();
    }

    @Test
    void createPersistsDetailAndReturnsDto() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(city));
        given(tripDetailRepository.save(any())).willAnswer(inv -> {
            TripDetailEntity e = inv.getArgument(0);
            e.setId(DETAIL_ID);
            return e;
        });

        TripDetailDto result = tripDetailService.create(caller, TRIP_ID,
                new TripDetailDto(null, null, null, VISIT_DATE, CITY_ID, 2));

        ArgumentCaptor<TripDetailEntity> captor = ArgumentCaptor.forClass(TripDetailEntity.class);
        verify(tripDetailRepository).save(captor.capture());
        TripDetailEntity saved = captor.getValue();
        assertThat(saved.getTrip()).isSameAs(trip);
        assertThat(saved.getUserId()).isEqualTo(CALLER_ID);
        assertThat(saved.getVisitDate()).isEqualTo(VISIT_DATE);
        assertThat(saved.getCity()).isSameAs(city);
        assertThat(saved.getCityOrder()).isEqualTo(2);

        assertThat(result.id()).isEqualTo(DETAIL_ID);
        assertThat(result.tripId()).isEqualTo(TRIP_ID);
        assertThat(result.userId()).isEqualTo(CALLER_ID);
        assertThat(result.visitDate()).isEqualTo(VISIT_DATE);
        assertThat(result.cityId()).isEqualTo(CITY_ID);
        assertThat(result.cityOrder()).isEqualTo(2);
    }

    @Test
    void createDefaultsCityOrderToMaxPlusOne() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(city));
        given(tripDetailRepository.findMaxCityOrder(TRIP_ID, VISIT_DATE)).willReturn(3);
        given(tripDetailRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        tripDetailService.create(caller, TRIP_ID,
                new TripDetailDto(null, null, null, VISIT_DATE, CITY_ID, null));

        ArgumentCaptor<TripDetailEntity> captor = ArgumentCaptor.forClass(TripDetailEntity.class);
        verify(tripDetailRepository).save(captor.capture());
        assertThat(captor.getValue().getCityOrder()).isEqualTo(4);
    }

    @Test
    void createDefaultsCityOrderToZeroWhenNoPreviousRows() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(city));
        given(tripDetailRepository.findMaxCityOrder(TRIP_ID, VISIT_DATE)).willReturn(-1);
        given(tripDetailRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        tripDetailService.create(caller, TRIP_ID,
                new TripDetailDto(null, null, null, VISIT_DATE, CITY_ID, null));

        ArgumentCaptor<TripDetailEntity> captor = ArgumentCaptor.forClass(TripDetailEntity.class);
        verify(tripDetailRepository).save(captor.capture());
        assertThat(captor.getValue().getCityOrder()).isEqualTo(0);
    }

    @Test
    void createThrows404WhenTripMissing() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailService.create(caller, TRIP_ID,
                new TripDetailDto(null, null, null, VISIT_DATE, CITY_ID, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripDetailRepository, never()).save(any());
    }

    @Test
    void createThrows404WhenTripOwnedByAnotherUser() {
        TripEntity otherTrip = TripEntity.builder().id(TRIP_ID).name("Other").user(otherUser).build();
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(otherTrip));

        assertThatThrownBy(() -> tripDetailService.create(caller, TRIP_ID,
                new TripDetailDto(null, null, null, VISIT_DATE, CITY_ID, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripDetailRepository, never()).save(any());
    }

    @Test
    void createThrows400WhenCityIdUnknown() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(cityRepository.findById(CITY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailService.create(caller, TRIP_ID,
                new TripDetailDto(null, null, null, VISIT_DATE, CITY_ID, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains(String.valueOf(CITY_ID));
                });
        verify(tripDetailRepository, never()).save(any());
    }
}
