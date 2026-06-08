package org.pinnel.pinnelapi.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.TripDetailPinDto;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.pinnel.pinnelapi.entity.TripDetailEntity;
import org.pinnel.pinnelapi.entity.TripDetailPinEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.PinRepository;
import org.pinnel.pinnelapi.repository.TripDetailPinRepository;
import org.pinnel.pinnelapi.repository.TripDetailRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TripDetailPinService {

    private final TripDetailRepository tripDetailRepository;
    private final TripDetailPinRepository tripDetailPinRepository;
    private final PinRepository pinRepository;

    /** Adds a pin to a city-on-a-day. pinOrder defaults to max+1 for that detail if omitted. Repeats allowed. */
    @Transactional
    public TripDetailPinDto add(UserEntity caller, Long detailId, TripDetailPinDto request) {
        TripDetailEntity detail = getDetailOwnedBy(caller, detailId);
        PinEntity pin = getPin(request.pinId());

        int order = request.pinOrder() != null
                ? request.pinOrder()
                : tripDetailPinRepository.findMaxPinOrder(detailId) + 1;

        TripDetailPinEntity saved = tripDetailPinRepository.save(TripDetailPinEntity.builder()
                .tripDetail(detail)
                .userId(caller.getId())
                .pin(pin)
                .pinOrder(order)
                .visitTime(request.visitTime())
                .budget(request.budget())
                .build());

        return TripDetailPinDto.from(saved);
    }

    /** Returns all pin entries for a detail ordered by pinOrder. Returns empty list if none exist. */
    public List<TripDetailPinDto> listByDetail(UserEntity caller, Long detailId) {
        getDetailOwnedBy(caller, detailId);
        return tripDetailPinRepository.findByTripDetail_IdAndUserIdOrderByPinOrder(detailId, caller.getId())
                .stream()
                .map(TripDetailPinDto::from)
                .toList();
    }

    /**
     * Moves a pin entry to a new pinOrder within its detail and renumbers all siblings to a dense 0..n-1 sequence.
     * Order is clamped silently if too large; negative order returns 400. 404 if missing or not the caller's.
     */
    @Transactional
    public TripDetailPinDto updateOrder(UserEntity caller, Long pinEntryId, int newOrder) {
        if (newOrder < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "order must be >= 0");
        }
        TripDetailPinEntity entry = tripDetailPinRepository.findById(pinEntryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!entry.getUserId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<TripDetailPinEntity> siblings = tripDetailPinRepository
                .findByTripDetail_IdAndUserIdOrderByPinOrder(entry.getTripDetail().getId(), caller.getId());

        int clamped = Math.min(newOrder, siblings.size() - 1);
        siblings.remove(entry);
        siblings.add(clamped, entry);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPinOrder(i);
        }
        tripDetailPinRepository.saveAll(siblings);

        return TripDetailPinDto.from(entry);
    }

    /** Deletes a single pin entry. Idempotent for missing; 404 if the row belongs to another user. */
    @Transactional
    public void delete(UserEntity caller, Long pinEntryId) {
        if (!tripDetailPinRepository.existsById(pinEntryId)) {
            return;
        }
        int deleted = tripDetailPinRepository.deleteByIdAndUserId(pinEntryId, caller.getId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private TripDetailEntity getDetailOwnedBy(UserEntity caller, Long detailId) {
        TripDetailEntity detail = tripDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!detail.getUserId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return detail;
    }

    private PinEntity getPin(Long pinId) {
        return pinRepository.findById(pinId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown pinId: " + pinId));
    }
}
