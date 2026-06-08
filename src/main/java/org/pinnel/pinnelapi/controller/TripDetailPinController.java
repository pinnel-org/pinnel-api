package org.pinnel.pinnelapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.auth.CurrentUser;
import org.pinnel.pinnelapi.dto.TripDetailPinDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.service.TripDetailPinService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TripDetailPinController {

    private final TripDetailPinService tripDetailPinService;

    /** POST /api/trip-details/{detailId}/pins — adds a pin to a city-on-a-day. Returns 201 with the created entry. */
    @PostMapping("/api/trip-details/{detailId}/pins")
    @ResponseStatus(HttpStatus.CREATED)
    public TripDetailPinDto add(@CurrentUser UserEntity caller,
                                @PathVariable Long detailId,
                                @Valid @RequestBody TripDetailPinDto request) {
        return tripDetailPinService.add(caller, detailId, request);
    }

    /** GET /api/trip-details/{detailId}/pins — lists all pin entries for a detail ordered by pinOrder. Returns 200 [] if none exist. */
    @GetMapping("/api/trip-details/{detailId}/pins")
    public List<TripDetailPinDto> listByDetail(@CurrentUser UserEntity caller,
                                               @PathVariable Long detailId) {
        return tripDetailPinService.listByDetail(caller, detailId);
    }

    /** PUT /api/trip-detail-pins/{id}/pin-order/{order} — reorders a pin within its detail; renumbers all siblings to 0..n-1. */
    @PutMapping("/api/trip-detail-pins/{id}/pin-order/{order}")
    public TripDetailPinDto updateOrder(@CurrentUser UserEntity caller,
                                        @PathVariable Long id,
                                        @PathVariable int order) {
        return tripDetailPinService.updateOrder(caller, id, order);
    }

    /** DELETE /api/trip-detail-pins/{id} — removes a single pin entry. 204 always; 404 if owned by another user. */
    @DeleteMapping("/api/trip-detail-pins/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser UserEntity caller, @PathVariable Long id) {
        tripDetailPinService.delete(caller, id);
    }
}
