package org.pinnel.pinnelapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.auth.CurrentUser;
import org.pinnel.pinnelapi.dto.PinDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.service.PinService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pins")
@RequiredArgsConstructor
public class PinController {

    private final PinService pinService;

    /** GET /api/pins?cityId={id} — lists pins in the city visible to the caller (curated + public + caller's own private). */
    @GetMapping
    public List<PinDto> listByCity(@CurrentUser UserEntity caller, @RequestParam Long cityId) {
        return pinService.listByCity(cityId, caller);
    }

    /** GET /api/pins/{id} — returns one pin if visible to the caller. 404 if not found or not visible. */
    @GetMapping("/{id}")
    public PinDto getById(@CurrentUser UserEntity caller, @PathVariable Long id) {
        return pinService.getById(id, caller);
    }

    /** POST /api/pins — creates a pin owned by the caller. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PinDto create(@CurrentUser UserEntity caller, @Valid @RequestBody PinDto request) {
        return pinService.create(caller, request);
    }

    /** PUT /api/pins/{id} — strict-replace update of an owned, still-private pin. 403 if curated, not owner, or already public. */
    @PutMapping("/{id}")
    public PinDto update(@CurrentUser UserEntity caller,
                         @PathVariable Long id,
                         @Valid @RequestBody PinDto request) {
        return pinService.update(caller, id, request);
    }

    /** DELETE /api/pins/{id} — deletes an owned, still-private pin. Returns 204. 403 if curated, not owner, or already public. Idempotent. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser UserEntity caller, @PathVariable Long id) {
        pinService.delete(caller, id);
    }
}
