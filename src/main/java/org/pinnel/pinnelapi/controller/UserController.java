package org.pinnel.pinnelapi.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.auth.CurrentUser;
import org.pinnel.pinnelapi.dto.UserDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/me — returns the authenticated user's profile. */
    @GetMapping
    public UserDto getCurrent(@CurrentUser UserEntity user) {
        return userService.getCurrentUser(user);
    }

    /** PUT /api/me — strict-replace update of the authenticated user's editable fields (username, displayName, bio). */
    @PutMapping
    public UserDto updateCurrent(@CurrentUser UserEntity user, @Valid @RequestBody UserDto update) {
        return userService.updateCurrentUser(user, update);
    }

    /** DELETE /api/me — deletes the authenticated user's account. Returns 204. Idempotent. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrent(@CurrentUser UserEntity user) {
        userService.deleteCurrentUser(user);
    }

    /** GET /api/me/countries — returns the distinct country names across all cities in the caller's trips. */
    @GetMapping("/countries")
    public Set<String> listMyCountries(@CurrentUser UserEntity user) {
        return userService.listMyCountries(user);
    }

    /** POST /api/me/avatar — replaces the authenticated user's avatar with the bytes of the uploaded 'file' part. Returns 204. */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadAvatar(@CurrentUser UserEntity user,
                             @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is empty");
        }
        userService.saveCurrentUserAvatar(user, file.getBytes());
    }

    /** GET /api/me/avatar — returns the authenticated user's avatar as image/jpeg, or 404 if none is set. */
    @GetMapping(value = "/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getAvatar(@CurrentUser UserEntity user) {
        return userService.getCurrentUserAvatar(user)
                .map(bytes -> ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** DELETE /api/me/avatar — clears the authenticated user's avatar. Returns 204. Idempotent. */
    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar(@CurrentUser UserEntity user) {
        userService.deleteCurrentUserAvatar(user);
    }
}
