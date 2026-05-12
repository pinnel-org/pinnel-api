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

    /** Lists the caller's trips. Each returned DTO includes the trip's current city/pin ids. */
    @Transactional(readOnly = true)
    public List<TripDto> listMine(UserEntity caller) {
        return tripRepository.findByUserId(caller.getId())
                .stream()
                .map(TripDto::from)
                .toList();
    }

    /** Returns one trip with its cities and pins. Throws 404 if the trip does not exist or does not belong to the caller. */
    @Transactional(readOnly = true)
    public TripDto getById(UserEntity caller, Long id) {
        return TripDto.from(getOwnTrip(caller, id));
    }

    /** Creates a trip owned by the caller. Cities and pins start empty — they are managed via separate endpoints. */
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

    /** Strict-replace of the trip's editable metadata (name, budget). Throws 404 if the trip does not exist or does not belong to the caller. */
    @Transactional
    public TripDto update(UserEntity caller, Long id, TripDto request) {
        TripEntity trip = getOwnTrip(caller, id);
        trip.setName(request.name());
        trip.setBudget(request.budget());
        trip.setUpdatedAt(Instant.now());
        return TripDto.from(tripRepository.save(trip));
    }

    /**
     * Deletes the trip. No-op if the trip does not exist (idempotent). Throws 404 if the trip exists
     * but does not belong to the caller (don't leak existence of other users' trips).
     */
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
