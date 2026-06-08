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

    private final TripRepository tripRepository;
    private final CityRepository cityRepository;
    private final PinRepository pinRepository;
    private final CityService cityService;

    @Transactional(readOnly = true)
    public List<TripDto> listMine(UserEntity caller) {
        return tripRepository.findByUserId(caller.getId())
                .stream()
                .map(TripDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripDto getById(UserEntity caller, Long id) {
        return TripDto.from(getOwnTrip(caller, id));
    }

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
                .pins(resolvePins(request.pinIds()))
                .coverImageUrl(resolveCoverImageUrl(cities))
                .build());
        return TripDto.from(saved);
    }

    @Transactional
    public TripDto update(UserEntity caller, Long id, TripDto request) {
        TripEntity trip = getOwnTrip(caller, id);
        trip.setName(request.name());
        trip.setBudget(request.budget());
        trip.setCities(resolveCities(request.cityIds()));
        trip.setPins(resolvePins(request.pinIds()));
        trip.setUpdatedAt(Instant.now());
        tripRepository.save(trip);
        return TripDto.from(trip);
    }

    @Transactional
    public void delete(UserEntity caller, Long id) {
        tripRepository.findById(id).ifPresent(trip -> {
            requireOwner(trip, caller);
            tripRepository.deleteById(id);
        });
    }

    private String resolveCoverImageUrl(Set<CityEntity> cities) {
        return cities.stream()
                .min(Comparator.comparing(CityEntity::getId))
                .map(cityService::buildCoverUrl)
                .orElse(null);
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

    private Set<PinEntity> resolvePins(Set<Long> ids) {
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
        return new HashSet<>(found);
    }
}
