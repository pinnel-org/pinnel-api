package org.pinnel.pinnelapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.auth.CurrentUser;
import org.pinnel.pinnelapi.dto.TripDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    /** GET /api/trips — lists the caller's trips, each with current city/pin ids. */
    @GetMapping
    public List<TripDto> listMine(@CurrentUser UserEntity caller) {
        return tripService.listMine(caller);
    }

    /** GET /api/trips/{id} — returns one of the caller's trips with its cities and pins. 404 if missing or not the caller's. */
    @GetMapping("/{id}")
    public TripDto getById(@CurrentUser UserEntity caller, @PathVariable Long id) {
        return tripService.getById(caller, id);
    }

    /** POST /api/trips — creates a trip owned by the caller. Cities/pins are managed separately. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripDto create(@CurrentUser UserEntity caller, @Valid @RequestBody TripDto request) {
        return tripService.create(caller, request);
    }

    /** PUT /api/trips/{id} — strict-replace update of name and budget. 404 if missing or not the caller's. */
    @PutMapping("/{id}")
    public TripDto update(@CurrentUser UserEntity caller,
                          @PathVariable Long id,
                          @Valid @RequestBody TripDto request) {
        return tripService.update(caller, id, request);
    }

    /** DELETE /api/trips/{id} — deletes the trip. Returns 204. Idempotent for missing trips; 404 for trips belonging to another user. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser UserEntity caller, @PathVariable Long id) {
        tripService.delete(caller, id);
    }
}
