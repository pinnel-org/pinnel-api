package org.pinnel.pinnelapi.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.TripDayCityDto;
import org.pinnel.pinnelapi.dto.TripDayDto;
import org.pinnel.pinnelapi.dto.TripDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.pinnel.pinnelapi.entity.TripDayCityEntity;
import org.pinnel.pinnelapi.entity.TripDayEntity;
import org.pinnel.pinnelapi.entity.TripDayPinEntity;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.pinnel.pinnelapi.repository.PinRepository;
import org.pinnel.pinnelapi.repository.TripDayRepository;
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
    private final TripDayRepository tripDayRepository;
    private final CityService cityService;

    /** Lists the caller's trips, each with city/pin ids and saved days. */
    @Transactional(readOnly = true)
    public List<TripDto> listMine(UserEntity caller) {
        return tripRepository.findByUserId(caller.getId())
                .stream()
                .map(trip -> TripDto.from(trip, tripDayRepository.findByTripIdWithDetails(trip.getId())))
                .toList();
    }

    /** Returns one of the caller's trips with cities, pins and saved days. 404 if missing or not the caller's. */
    @Transactional(readOnly = true)
    public TripDto getById(UserEntity caller, Long id) {
        TripEntity trip = getOwnTrip(caller, id);
        return TripDto.from(trip, tripDayRepository.findByTripIdWithDetails(trip.getId()));
    }

    /** Creates a trip owned by the caller. cityIds / pinIds are attached; days may be omitted on creation. */
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
        List<TripDayEntity> days = saveDays(saved, request.days());
        return TripDto.from(saved, days);
    }

    /** Strict-replace of name, budget, cityIds, pinIds and days. 404 if missing or not the caller's. */
    @Transactional
    public TripDto update(UserEntity caller, Long id, TripDto request) {
        TripEntity trip = getOwnTrip(caller, id);
        trip.setName(request.name());
        trip.setBudget(request.budget());
        trip.setCities(resolveCities(request.cityIds()));
        trip.setPins(resolvePins(request.pinIds()));
        trip.setUpdatedAt(Instant.now());
        tripRepository.save(trip);

        tripDayRepository.deleteByTripId(trip.getId());
        tripDayRepository.flush();

        List<TripDayEntity> days = saveDays(trip, request.days());
        return TripDto.from(trip, days);
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

    private List<TripDayEntity> saveDays(TripEntity trip, List<TripDayDto> dayDtos) {
        if (dayDtos == null || dayDtos.isEmpty()) {
            return List.of();
        }

        Set<Long> allCityIds = dayDtos.stream()
                .flatMap(d -> d.cities().stream())
                .map(TripDayCityDto::cityId)
                .collect(Collectors.toSet());

        Set<Long> allPinIds = dayDtos.stream()
                .flatMap(d -> d.cities().stream())
                .flatMap(c -> c.pinIds().stream())
                .collect(Collectors.toSet());

        Map<Long, CityEntity> cityMap = cityRepository.findAllById(allCityIds)
                .stream().collect(Collectors.toMap(CityEntity::getId, c -> c));

        Map<Long, PinEntity> pinMap = allPinIds.isEmpty() ? Map.of() :
                pinRepository.findAllById(allPinIds)
                        .stream().collect(Collectors.toMap(PinEntity::getId, p -> p));

        validateIds(allCityIds, cityMap.keySet(), "cityIds");
        validateIds(allPinIds, pinMap.keySet(), "pinIds");

        List<TripDayEntity> entities = dayDtos.stream().map(dto -> {
            TripDayEntity day = TripDayEntity.builder()
                    .trip(trip)
                    .visitDate(dto.visitDate())
                    .build();

            List<TripDayCityEntity> cities = IntStream.range(0, dto.cities().size())
                    .mapToObj(ci -> {
                        TripDayCityDto cityDto = dto.cities().get(ci);
                        TripDayCityEntity dayCity = TripDayCityEntity.builder()
                                .tripDay(day)
                                .city(cityMap.get(cityDto.cityId()))
                                .cityOrder(ci)
                                .build();

                        List<TripDayPinEntity> pins = IntStream.range(0, cityDto.pinIds().size())
                                .mapToObj(pi -> TripDayPinEntity.builder()
                                        .tripDayCity(dayCity)
                                        .pin(pinMap.get(cityDto.pinIds().get(pi)))
                                        .pinOrder(pi)
                                        .build())
                                .toList();

                        dayCity.getPins().addAll(pins);
                        return dayCity;
                    })
                    .toList();

            day.getCities().addAll(cities);
            return day;
        }).toList();

        return tripDayRepository.saveAll(entities);
    }

    private void validateIds(Set<Long> requested, Set<Long> found, String field) {
        if (requested.size() == found.size()) return;
        Set<Long> missing = new TreeSet<>(requested);
        missing.removeAll(found);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown " + field + ": " + missing);
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
        if (ids.isEmpty()) return new HashSet<>();
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
        if (ids.isEmpty()) return new HashSet<>();
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
