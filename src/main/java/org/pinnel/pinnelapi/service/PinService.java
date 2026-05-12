package org.pinnel.pinnelapi.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.PinDto;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.pinnel.pinnelapi.repository.PinRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PinService {

    private final PinRepository pinRepository;
    private final CityRepository cityRepository;

    /** Lists pins in a city visible to the caller: curated pins, public user-created pins, and the caller's own private pins. */
    public List<PinDto> listByCity(Long cityId, UserEntity caller) {
        return pinRepository.findVisibleByCityId(cityId, caller.getId())
                .stream()
                .map(PinDto::from)
                .toList();
    }

    /** Returns the pin with the given id if it is visible to the caller (curated, public, or owned by the caller). Throws 404 otherwise. */
    public PinDto getById(Long id, UserEntity caller) {
        PinEntity pin = getPin(id);
        if (!isVisibleTo(pin, caller)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return PinDto.from(pin);
    }

    /** Creates a pin owned by the caller in the referenced city. Throws 404 if the city does not exist. */
    @Transactional
    public PinDto create(UserEntity caller, PinDto request) {
        CityEntity city = getCity(request.cityId());
        Instant now = Instant.now();
        PinEntity saved = pinRepository.save(PinEntity.builder()
                .name(request.name())
                .description(request.description())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .isPublic(request.isPublic())
                .city(city)
                .user(caller)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return PinDto.from(saved);
    }

    /**
     * Strict-replace update of an owned, still-private pin. Throws 404 if the pin does not exist,
     * 403 if the caller is not the owner or the pin is already public (one-way submission — once
     * public, only support can modify it).
     */
    @Transactional
    public PinDto update(UserEntity caller, Long id, PinDto request) {
        PinEntity pin = getPin(id);
        requireEditable(pin, caller);

        CityEntity city = pin.getCity().getId().equals(request.cityId())
                ? pin.getCity()
                : getCity(request.cityId());

        pin.setName(request.name());
        pin.setDescription(request.description());
        pin.setLatitude(request.latitude());
        pin.setLongitude(request.longitude());
        pin.setPublic(request.isPublic());
        pin.setCity(city);
        pin.setUpdatedAt(Instant.now());
        return PinDto.from(pinRepository.save(pin));
    }

    /**
     * Deletes an owned, still-private pin. Throws 403 if the pin is curated, owned by someone else, or already public.
     * If the pin does not exist, returns silently (idempotent).
     */
    @Transactional
    public void delete(UserEntity caller, Long id) {
        pinRepository.findById(id).ifPresent(pin -> {
            requireEditable(pin, caller);
            pinRepository.deleteById(id);
        });
    }

    private PinEntity getPin(Long id) {
        return pinRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CityEntity getCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found"));
    }

    private boolean isVisibleTo(PinEntity pin, UserEntity caller) {
        if (pin.getUser() == null || pin.isPublic()) {
            return true;
        }
        return pin.getUser().getId().equals(caller.getId());
    }

    private void requireEditable(PinEntity pin, UserEntity caller) {
        if (pin.getUser() == null || !pin.getUser().getId().equals(caller.getId()) || pin.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
