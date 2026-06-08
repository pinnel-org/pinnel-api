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

    /** Returns all details for a trip ordered by visitDate then cityOrder. Returns empty list if none exist. */
    public List<TripDetailDto> listAll(UserEntity caller, Long tripId) {
        getTripOwnedBy(caller, tripId);
        return tripDetailRepository.findByTrip_IdAndUserIdOrderByVisitDateAscCityOrderAsc(tripId, caller.getId())
                .stream()
                .map(TripDetailDto::from)
                .toList();
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
    public void delete(UserEntity caller, Long detailId) {
        if (!tripDetailRepository.existsById(detailId)) {
            return;
        }
        int deleted = tripDetailRepository.deleteByIdAndUserId(detailId, caller.getId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Moves a trip_detail to a new cityOrder within its day and renumbers all siblings to a dense 0..n-1 sequence.
     * Order is clamped silently if too large; negative order returns 400. 404 if missing or not the caller's.
     */
    @Transactional
    public TripDetailDto updateOrder(UserEntity caller, Long detailId, int newOrder) {
        if (newOrder < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "order must be >= 0");
        }
        TripDetailEntity detail = tripDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!detail.getUserId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<TripDetailEntity> siblings = tripDetailRepository
                .findByTrip_IdAndUserIdAndVisitDateOrderByCityOrder(
                        detail.getTrip().getId(), caller.getId(), detail.getVisitDate());

        int clamped = Math.min(newOrder, siblings.size() - 1);
        siblings.remove(detail);
        siblings.add(clamped, detail);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setCityOrder(i);
        }
        tripDetailRepository.saveAll(siblings);

        return TripDetailDto.from(detail);
    }

    /** Bulk-deletes all details for a given (tripId, date). 404 if trip missing or not the caller's. */
    @Transactional
    public void deleteByDate(UserEntity caller, Long tripId, LocalDate date) {
        getTripOwnedBy(caller, tripId);
        tripDetailRepository.deleteByTripIdAndUserIdAndVisitDate(tripId, caller.getId(), date);
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
