package org.pinnel.pinnelapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
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
    void listAllReturnsAllDetailsOrderedByVisitDateThenCityOrder() {
        TripDetailEntity d1 = TripDetailEntity.builder().id(1L).trip(trip).userId(CALLER_ID)
                .visitDate(VISIT_DATE).city(city).cityOrder(0).build();
        TripDetailEntity d2 = TripDetailEntity.builder().id(2L).trip(trip).userId(CALLER_ID)
                .visitDate(VISIT_DATE.plusDays(1)).city(city).cityOrder(0).build();
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(tripDetailRepository.findByTrip_IdAndUserIdOrderByVisitDateAscCityOrderAsc(TRIP_ID, CALLER_ID))
                .willReturn(List.of(d1, d2));

        List<TripDetailDto> result = tripDetailService.listAll(caller, TRIP_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    void listAllReturnsEmptyListWhenNoDetails() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(tripDetailRepository.findByTrip_IdAndUserIdOrderByVisitDateAscCityOrderAsc(TRIP_ID, CALLER_ID))
                .willReturn(List.of());

        List<TripDetailDto> result = tripDetailService.listAll(caller, TRIP_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void listAllThrows404WhenTripMissing() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailService.listAll(caller, TRIP_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listAllThrows404WhenTripOwnedByAnotherUser() {
        TripEntity otherTrip = TripEntity.builder().id(TRIP_ID).name("Other").user(otherUser).build();
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(otherTrip));

        assertThatThrownBy(() -> tripDetailService.listAll(caller, TRIP_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listByDateReturnsDetailsOrderedByCityOrder() {
        TripDetailEntity d1 = TripDetailEntity.builder().id(1L).trip(trip).userId(CALLER_ID)
                .visitDate(VISIT_DATE).city(city).cityOrder(0).build();
        TripDetailEntity d2 = TripDetailEntity.builder().id(2L).trip(trip).userId(CALLER_ID)
                .visitDate(VISIT_DATE).city(city).cityOrder(1).build();
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(tripDetailRepository.findByTrip_IdAndUserIdAndVisitDateOrderByCityOrder(TRIP_ID, CALLER_ID, VISIT_DATE))
                .willReturn(List.of(d1, d2));

        List<TripDetailDto> result = tripDetailService.listByDate(caller, TRIP_ID, VISIT_DATE);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    void listByDateReturnsEmptyListWhenDayNotPersisted() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));
        given(tripDetailRepository.findByTrip_IdAndUserIdAndVisitDateOrderByCityOrder(TRIP_ID, CALLER_ID, VISIT_DATE))
                .willReturn(List.of());

        List<TripDetailDto> result = tripDetailService.listByDate(caller, TRIP_ID, VISIT_DATE);

        assertThat(result).isEmpty();
    }

    @Test
    void listByDateThrows404WhenTripMissing() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailService.listByDate(caller, TRIP_ID, VISIT_DATE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listByDateThrows404WhenTripOwnedByAnotherUser() {
        TripEntity otherTrip = TripEntity.builder().id(TRIP_ID).name("Other").user(otherUser).build();
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(otherTrip));

        assertThatThrownBy(() -> tripDetailService.listByDate(caller, TRIP_ID, VISIT_DATE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
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

    @Test
    void updateOrderMovesDetailAndRenumbersSiblings() {
        TripDetailEntity d0 = buildDetail(1L, 0);
        TripDetailEntity d1 = buildDetail(DETAIL_ID, 1);
        TripDetailEntity d2 = buildDetail(3L, 2);
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(d1));
        given(tripDetailRepository.findByTrip_IdAndUserIdAndVisitDateOrderByCityOrder(TRIP_ID, CALLER_ID, VISIT_DATE))
                .willReturn(new java.util.ArrayList<>(List.of(d0, d1, d2)));

        TripDetailDto result = tripDetailService.updateOrder(caller, DETAIL_ID, 0);

        assertThat(d1.getCityOrder()).isEqualTo(0);
        assertThat(d0.getCityOrder()).isEqualTo(1);
        assertThat(d2.getCityOrder()).isEqualTo(2);
        assertThat(result.cityOrder()).isEqualTo(0);
        verify(tripDetailRepository).saveAll(any());
    }

    @Test
    void updateOrderClampsWhenOrderExceedsBound() {
        TripDetailEntity d0 = buildDetail(1L, 0);
        TripDetailEntity d1 = buildDetail(DETAIL_ID, 1);
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(d1));
        given(tripDetailRepository.findByTrip_IdAndUserIdAndVisitDateOrderByCityOrder(TRIP_ID, CALLER_ID, VISIT_DATE))
                .willReturn(new java.util.ArrayList<>(List.of(d0, d1)));

        tripDetailService.updateOrder(caller, DETAIL_ID, 99);

        assertThat(d1.getCityOrder()).isEqualTo(1);
        assertThat(d0.getCityOrder()).isEqualTo(0);
    }

    @Test
    void updateOrderThrows400WhenOrderNegative() {
        assertThatThrownBy(() -> tripDetailService.updateOrder(caller, DETAIL_ID, -1))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(tripDetailRepository, never()).findById(any());
    }

    @Test
    void updateOrderThrows404WhenDetailMissing() {
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailService.updateOrder(caller, DETAIL_ID, 0))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateOrderThrows404WhenOwnedByAnotherUser() {
        TripDetailEntity detail = buildDetail(DETAIL_ID, 0);
        detail.setUserId(OTHER_USER_ID);
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(detail));

        assertThatThrownBy(() -> tripDetailService.updateOrder(caller, DETAIL_ID, 0))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripDetailRepository, never()).saveAll(any());
    }

    private TripDetailEntity buildDetail(Long id, int order) {
        return TripDetailEntity.builder().id(id).trip(trip).userId(CALLER_ID)
                .visitDate(VISIT_DATE).city(city).cityOrder(order).build();
    }

    @Test
    void deleteRemovesDetailWhenOwner() {
        given(tripDetailRepository.existsById(DETAIL_ID)).willReturn(true);
        given(tripDetailRepository.deleteByIdAndUserId(DETAIL_ID, CALLER_ID)).willReturn(1);

        tripDetailService.delete(caller, DETAIL_ID);

        verify(tripDetailRepository).deleteByIdAndUserId(DETAIL_ID, CALLER_ID);
    }

    @Test
    void deleteIsIdempotentWhenMissing() {
        given(tripDetailRepository.existsById(DETAIL_ID)).willReturn(false);

        tripDetailService.delete(caller, DETAIL_ID);

        verify(tripDetailRepository, never()).deleteByIdAndUserId(any(), any());
    }

    @Test
    void deleteThrows404WhenOwnedByAnotherUser() {
        given(tripDetailRepository.existsById(DETAIL_ID)).willReturn(true);
        given(tripDetailRepository.deleteByIdAndUserId(DETAIL_ID, CALLER_ID)).willReturn(0);

        assertThatThrownBy(() -> tripDetailService.delete(caller, DETAIL_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteByDateDeletesAllDetailsForDay() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(trip));

        tripDetailService.deleteByDate(caller, TRIP_ID, VISIT_DATE);

        verify(tripDetailRepository).deleteByTripIdAndUserIdAndVisitDate(TRIP_ID, CALLER_ID, VISIT_DATE);
    }

    @Test
    void deleteByDateThrows404WhenTripMissing() {
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailService.deleteByDate(caller, TRIP_ID, VISIT_DATE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripDetailRepository, never()).deleteByTripIdAndUserIdAndVisitDate(any(), any(), any());
    }

    @Test
    void deleteByDateThrows404WhenTripOwnedByAnotherUser() {
        TripEntity otherTrip = TripEntity.builder().id(TRIP_ID).name("Other").user(otherUser).build();
        given(tripRepository.findById(TRIP_ID)).willReturn(Optional.of(otherTrip));

        assertThatThrownBy(() -> tripDetailService.deleteByDate(caller, TRIP_ID, VISIT_DATE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripDetailRepository, never()).deleteByTripIdAndUserIdAndVisitDate(any(), any(), any());
    }
}
