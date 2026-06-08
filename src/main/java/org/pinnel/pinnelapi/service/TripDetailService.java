package org.pinnel.pinnelapi.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.TripDetailDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.TripDetailEntity;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.pinnel.pinnelapi.repository.TripDetailRepository;
import org.pinnel.pinnelapi.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TripDetailService {

    private final TripRepository tripRepository;
    private final CityRepository cityRepository;
    private final TripDetailRepository tripDetailRepository;

    /** Creates a trip_details row (city on a date). cityOrder defaults to max+1 for that (tripId, date) if omitted. */
    @Transactional
    public TripDetailDto create(UserEntity caller, Long tripId, TripDetailDto request) {
        TripEntity trip = getTripOwnedBy(caller, tripId);
        CityEntity city = getCity(request.cityId());

        int order = request.cityOrder() != null
                ? request.cityOrder()
                : tripDetailRepository.findMaxCityOrder(tripId, request.visitDate()) + 1;

        TripDetailEntity saved = tripDetailRepository.save(TripDetailEntity.builder()
                .trip(trip)
                .userId(caller.getId())
                .visitDate(request.visitDate())
                .city(city)
                .cityOrder(order)
                .build());

        return TripDetailDto.from(saved);
    }

    /** Returns details for a day ordered by cityOrder. Returns empty list if the day was never persisted. */
    public List<TripDetailDto> listByDate(UserEntity caller, Long tripId, LocalDate date) {
        getTripOwnedBy(caller, tripId);
        return tripDetailRepository.findByTrip_IdAndUserIdAndVisitDateOrderByCityOrder(tripId, caller.getId(), date)
                .stream()
                .map(TripDetailDto::from)
                .toList();
    }

    /** Deletes a single trip_detail. Idempotent for missing; 404 if the row belongs to another user. */
    @Transactional
    public void deleteSingle(UserEntity caller, Long detailId) {
        tripDetailRepository.findById(detailId).ifPresent(detail -> {
            if (!detail.getUserId().equals(caller.getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            tripDetailRepository.deleteById(detailId);
        });
    }

    /** Bulk-deletes all details for a given (tripId, date). 404 if trip missing or not the caller's. */
    @Transactional
    public void deleteByDate(UserEntity caller, Long tripId, LocalDate date) {
        getTripOwnedBy(caller, tripId);
        tripDetailRepository.deleteByTripAndUserAndDate(tripId, caller.getId(), date);
    }

    private TripEntity getTripOwnedBy(UserEntity caller, Long tripId) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!trip.getUser().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return trip;
    }

    private CityEntity getCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown cityId: " + cityId));
    }
}
