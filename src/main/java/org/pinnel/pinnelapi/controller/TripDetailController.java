package org.pinnel.pinnelapi.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.auth.CurrentUser;
import org.pinnel.pinnelapi.dto.TripDetailDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.service.TripDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TripDetailController {

    private final TripDetailService tripDetailService;

    /** DELETE /api/trip-details/{detailId} — removes a single detail. 204 always; 404 if owned by another user. */
    @DeleteMapping("/api/trip-details/{detailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSingle(@CurrentUser UserEntity caller, @PathVariable Long detailId) {
        tripDetailService.deleteSingle(caller, detailId);
    }

    /** DELETE /api/trips/{tripId}/trip-details?date={date} — bulk-removes all details for a day. 204 always; 404 if trip missing or not caller's. */
    @DeleteMapping("/api/trips/{tripId}/trip-details")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByDate(@CurrentUser UserEntity caller,
                             @PathVariable Long tripId,
                             @RequestParam LocalDate date) {
        tripDetailService.deleteByDate(caller, tripId, date);
    }

    /** GET /api/trips/{tripId}/trip-details?date={date} — lists details for a day ordered by cityOrder. Returns 200 [] if the day was never persisted. */
    @GetMapping("/api/trips/{tripId}/trip-details")
    public List<TripDetailDto> listByDate(@CurrentUser UserEntity caller,
                                          @PathVariable Long tripId,
                                          @RequestParam LocalDate date) {
        return tripDetailService.listByDate(caller, tripId, date);
    }

    /** POST /api/trips/{tripId}/trip-details — adds a city to a date of a trip. Returns 201 with the created detail. */
    @PostMapping("/api/trips/{tripId}/trip-details")
    @ResponseStatus(HttpStatus.CREATED)
    public TripDetailDto create(@CurrentUser UserEntity caller,
                                @PathVariable Long tripId,
                                @Valid @RequestBody TripDetailDto request) {
        return tripDetailService.create(caller, tripId, request);
    }
}
