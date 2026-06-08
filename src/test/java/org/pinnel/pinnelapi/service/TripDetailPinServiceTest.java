package org.pinnel.pinnelapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pinnel.pinnelapi.dto.TripDetailPinDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.pinnel.pinnelapi.entity.TripDetailEntity;
import org.pinnel.pinnelapi.entity.TripDetailPinEntity;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.PinRepository;
import org.pinnel.pinnelapi.repository.TripDetailPinRepository;
import org.pinnel.pinnelapi.repository.TripDetailRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TripDetailPinServiceTest {

    private static final Long CALLER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long DETAIL_ID = 10L;
    private static final Long PIN_ID = 20L;
    private static final Long ENTRY_ID = 100L;

    @Mock private TripDetailRepository tripDetailRepository;
    @Mock private TripDetailPinRepository tripDetailPinRepository;
    @Mock private PinRepository pinRepository;

    @InjectMocks private TripDetailPinService tripDetailPinService;

    private UserEntity caller;
    private TripDetailEntity detail;
    private PinEntity pin;

    @BeforeEach
    void setUp() {
        caller = UserEntity.builder().id(CALLER_ID).cognitoId("caller").username("caller").build();
        TripEntity trip = TripEntity.builder().id(1L).name("Rome").user(caller).build();
        CityEntity city = CityEntity.builder().id(1L).name("Rome").country("Italy").build();
        detail = TripDetailEntity.builder().id(DETAIL_ID).trip(trip).userId(CALLER_ID)
                .city(city).cityOrder(0).build();
        pin = PinEntity.builder().id(PIN_ID).name("Colosseum").build();
    }

    @Test
    void addPersistsEntryAndReturnsDto() {
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(detail));
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin));
        given(tripDetailPinRepository.save(any())).willAnswer(inv -> {
            TripDetailPinEntity e = inv.getArgument(0);
            e.setId(ENTRY_ID);
            return e;
        });

        TripDetailPinDto result = tripDetailPinService.add(caller, DETAIL_ID,
                new TripDetailPinDto(null, null, null, PIN_ID, 2,
                        LocalTime.of(10, 30), new BigDecimal("25.00")));

        ArgumentCaptor<TripDetailPinEntity> captor = ArgumentCaptor.forClass(TripDetailPinEntity.class);
        verify(tripDetailPinRepository).save(captor.capture());
        TripDetailPinEntity saved = captor.getValue();
        assertThat(saved.getTripDetail()).isSameAs(detail);
        assertThat(saved.getUserId()).isEqualTo(CALLER_ID);
        assertThat(saved.getPin()).isSameAs(pin);
        assertThat(saved.getPinOrder()).isEqualTo(2);
        assertThat(saved.getVisitTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(saved.getBudget()).isEqualByComparingTo("25.00");

        assertThat(result.id()).isEqualTo(ENTRY_ID);
        assertThat(result.tripDetailId()).isEqualTo(DETAIL_ID);
        assertThat(result.userId()).isEqualTo(CALLER_ID);
        assertThat(result.pinId()).isEqualTo(PIN_ID);
        assertThat(result.pinOrder()).isEqualTo(2);
    }

    @Test
    void addDefaultsPinOrderToMaxPlusOne() {
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(detail));
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin));
        given(tripDetailPinRepository.findMaxPinOrder(DETAIL_ID)).willReturn(3);
        given(tripDetailPinRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        tripDetailPinService.add(caller, DETAIL_ID,
                new TripDetailPinDto(null, null, null, PIN_ID, null, null, null));

        ArgumentCaptor<TripDetailPinEntity> captor = ArgumentCaptor.forClass(TripDetailPinEntity.class);
        verify(tripDetailPinRepository).save(captor.capture());
        assertThat(captor.getValue().getPinOrder()).isEqualTo(4);
    }

    @Test
    void addDefaultsPinOrderToZeroWhenNoPreviousRows() {
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(detail));
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.of(pin));
        given(tripDetailPinRepository.findMaxPinOrder(DETAIL_ID)).willReturn(-1);
        given(tripDetailPinRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        tripDetailPinService.add(caller, DETAIL_ID,
                new TripDetailPinDto(null, null, null, PIN_ID, null, null, null));

        ArgumentCaptor<TripDetailPinEntity> captor = ArgumentCaptor.forClass(TripDetailPinEntity.class);
        verify(tripDetailPinRepository).save(captor.capture());
        assertThat(captor.getValue().getPinOrder()).isEqualTo(0);
    }

    @Test
    void addThrows404WhenDetailMissing() {
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailPinService.add(caller, DETAIL_ID,
                new TripDetailPinDto(null, null, null, PIN_ID, null, null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripDetailPinRepository, never()).save(any());
    }

    @Test
    void addThrows404WhenDetailOwnedByAnotherUser() {
        TripDetailEntity otherDetail = TripDetailEntity.builder().id(DETAIL_ID)
                .userId(OTHER_USER_ID).build();
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(otherDetail));

        assertThatThrownBy(() -> tripDetailPinService.add(caller, DETAIL_ID,
                new TripDetailPinDto(null, null, null, PIN_ID, null, null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tripDetailPinRepository, never()).save(any());
    }

    @Test
    void addThrows400WhenPinIdUnknown() {
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(detail));
        given(pinRepository.findById(PIN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailPinService.add(caller, DETAIL_ID,
                new TripDetailPinDto(null, null, null, PIN_ID, null, null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains(String.valueOf(PIN_ID));
                });
        verify(tripDetailPinRepository, never()).save(any());
    }

    @Test
    void listByDetailReturnsEntriesOrderedByPinOrder() {
        TripDetailPinEntity e1 = buildEntry(1L, 0);
        TripDetailPinEntity e2 = buildEntry(2L, 1);
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(detail));
        given(tripDetailPinRepository.findByTripDetail_IdAndUserIdOrderByPinOrder(DETAIL_ID, CALLER_ID))
                .willReturn(List.of(e1, e2));

        List<TripDetailPinDto> result = tripDetailPinService.listByDetail(caller, DETAIL_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    void listByDetailReturnsEmptyListWhenNoPins() {
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(detail));
        given(tripDetailPinRepository.findByTripDetail_IdAndUserIdOrderByPinOrder(DETAIL_ID, CALLER_ID))
                .willReturn(List.of());

        List<TripDetailPinDto> result = tripDetailPinService.listByDetail(caller, DETAIL_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void listByDetailThrows404WhenDetailMissing() {
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripDetailPinService.listByDetail(caller, DETAIL_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listByDetailThrows404WhenDetailOwnedByAnotherUser() {
        TripDetailEntity otherDetail = TripDetailEntity.builder().id(DETAIL_ID)
                .userId(OTHER_USER_ID).build();
        given(tripDetailRepository.findById(DETAIL_ID)).willReturn(Optional.of(otherDetail));

        assertThatThrownBy(() -> tripDetailPinService.listByDetail(caller, DETAIL_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteRemovesEntryWhenOwner() {
        given(tripDetailPinRepository.existsById(ENTRY_ID)).willReturn(true);
        given(tripDetailPinRepository.deleteByIdAndUserId(ENTRY_ID, CALLER_ID)).willReturn(1);

        tripDetailPinService.delete(caller, ENTRY_ID);

        verify(tripDetailPinRepository).deleteByIdAndUserId(ENTRY_ID, CALLER_ID);
    }

    @Test
    void deleteIsIdempotentWhenMissing() {
        given(tripDetailPinRepository.existsById(ENTRY_ID)).willReturn(false);

        tripDetailPinService.delete(caller, ENTRY_ID);

        verify(tripDetailPinRepository, never()).deleteByIdAndUserId(any(), any());
    }

    @Test
    void deleteThrows404WhenOwnedByAnotherUser() {
        given(tripDetailPinRepository.existsById(ENTRY_ID)).willReturn(true);
        given(tripDetailPinRepository.deleteByIdAndUserId(ENTRY_ID, CALLER_ID)).willReturn(0);

        assertThatThrownBy(() -> tripDetailPinService.delete(caller, ENTRY_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private TripDetailPinEntity buildEntry(Long id, int order) {
        return TripDetailPinEntity.builder()
                .id(id)
                .tripDetail(detail)
                .userId(CALLER_ID)
                .pin(pin)
                .pinOrder(order)
                .build();
    }
}
