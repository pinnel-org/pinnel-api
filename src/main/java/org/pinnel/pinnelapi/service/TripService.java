package org.pinnel.pinnelapi.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.TripDto;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;

    /** Returns all trips owned by the caller ordered by repository default. */
    @Transactional(readOnly = true)
    public List<TripDto> listMine(UserEntity caller) {
        return tripRepository.findByUserId(caller.getId())
                .stream()
                .map(TripDto::from)
                .toList();
    }

    /** Returns the caller's trip by id. 404 if missing or not the caller's. */
    @Transactional(readOnly = true)
    public TripDto getById(UserEntity caller, Long id) {
        return TripDto.from(getOwnTrip(caller, id));
    }

    /** Creates a new trip owned by the caller. */
    @Transactional
    public TripDto create(UserEntity caller, TripDto request) {
        Instant now = Instant.now();
        TripEntity saved = tripRepository.save(TripEntity.builder()
                .name(request.name())
                .budget(request.budget())
                .user(caller)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return TripDto.from(saved);
    }

    /** Replaces the editable fields (name, budget) of the caller's trip. 404 if missing or not the caller's. */
    @Transactional
    public TripDto update(UserEntity caller, Long id, TripDto request) {
        TripEntity trip = getOwnTrip(caller, id);
        trip.setName(request.name());
        trip.setBudget(request.budget());
        trip.setUpdatedAt(Instant.now());
        tripRepository.save(trip);
        return TripDto.from(trip);
    }

    /** Deletes the caller's trip. Idempotent — does nothing if the trip is missing. 404 if owned by another user. */
    @Transactional
    public void delete(UserEntity caller, Long id) {
        tripRepository.findById(id).ifPresent(trip -> {
            requireOwner(trip, caller);
            tripRepository.deleteById(id);
        });
    }

    private TripEntity getOwnTrip(UserEntity caller, Long id) {
        TripEntity trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireOwner(trip, caller);
        return trip;
    }

    private void requireOwner(TripEntity trip, UserEntity caller) {
        if (!trip.getUser().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
