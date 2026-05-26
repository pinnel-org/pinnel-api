package org.pinnel.pinnelapi.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.TripDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.pinnel.pinnelapi.repository.PinRepository;
import org.pinnel.pinnelapi.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TripService {

    /** Sentinel for "no trip to exclude" in private-pin uniqueness checks — no real trip will ever have this id. */
    private static final long NO_TRIP_TO_EXCLUDE = -1L;

    private final TripRepository tripRepository;
    private final CityRepository cityRepository;
    private final PinRepository pinRepository;
    private final CityService cityService;

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

    /** Creates a trip owned by the caller. cityIds / pinIds in the request are attached; unknown ids → 400. A private pin already attached to another trip → 400. coverImageUrl is set from a random cover of the lowest-id attached city, or null if no cities are attached. */
    @Transactional
    public TripDto create(UserEntity caller, TripDto request) {
        Instant now = Instant.now();
        Set<CityEntity> cities = resolveCities(request.cityIds());
        TripEntity saved = tripRepository.save(TripEntity.builder()
                .name(request.name())
                .budget(request.budget())
                .user(caller)
                .createdAt(now)
                .updatedAt(now)
                .cities(cities)
                .pins(resolvePins(request.pinIds(), NO_TRIP_TO_EXCLUDE))
                .coverImageUrl(resolveCoverImageUrl(cities))
                .build());
        return TripDto.from(saved);
    }

    private String resolveCoverImageUrl(Set<CityEntity> cities) {
        return cities.stream()
                .min(Comparator.comparing(CityEntity::getId))
                .map(cityService::buildCoverUrl)
                .orElse(null);
    }

    /** Strict-replace of the trip's editable fields (name, budget, cityIds, pinIds). Throws 404 if missing / not the caller's, 400 if any city or pin id is unknown, or if a submitted private pin is already attached to a different trip. */
    @Transactional
    public TripDto update(UserEntity caller, Long id, TripDto request) {
        TripEntity trip = getOwnTrip(caller, id);
        trip.setName(request.name());
        trip.setBudget(request.budget());
        trip.setCities(resolveCities(request.cityIds()));
        trip.setPins(resolvePins(request.pinIds(), trip.getId()));
        trip.setUpdatedAt(Instant.now());
        return TripDto.from(tripRepository.save(trip));
    }

    /**
     * Deletes the trip and the caller's private pins that were attached to it. No-op if the trip
     * does not exist (idempotent). Throws 404 if the trip exists but does not belong to the caller
     * (don't leak existence of other users' trips). Public and curated pins on the trip are retained.
     */
    @Transactional
    public void delete(UserEntity caller, Long id) {
        tripRepository.findById(id).ifPresent(trip -> {
            requireOwner(trip, caller);
            Set<PinEntity> attached = new HashSet<>(trip.getPins());
            tripRepository.deleteById(id);
            tripRepository.flush();
            for (PinEntity pin : attached) {
                if (!pin.isPublic()
                        && pin.getUser() != null
                        && pin.getUser().getId().equals(caller.getId())) {
                    pinRepository.delete(pin);
                }
            }
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

    private Set<CityEntity> resolveCities(Set<Long> ids) {
        if (ids.isEmpty()) {
            return new HashSet<>();
        }
        List<CityEntity> found = cityRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            Set<Long> foundIds = found.stream().map(CityEntity::getId).collect(Collectors.toSet());
            Set<Long> missing = new TreeSet<>(ids);
            missing.removeAll(foundIds);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown cityIds: " + missing);
        }
        return new HashSet<>(found);
    }

    private Set<PinEntity> resolvePins(Set<Long> ids, long excludeTripId) {
        if (ids.isEmpty()) {
            return new HashSet<>();
        }
        List<PinEntity> found = pinRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            Set<Long> foundIds = found.stream().map(PinEntity::getId).collect(Collectors.toSet());
            Set<Long> missing = new TreeSet<>(ids);
            missing.removeAll(foundIds);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown pinIds: " + missing);
        }
        for (PinEntity pin : found) {
            if (!pin.isPublic()
                    && tripRepository.existsOtherTripContainingPin(pin.getId(), excludeTripId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Private pin already attached to another trip: " + pin.getId());
            }
        }
        return new HashSet<>(found);
    }
}
