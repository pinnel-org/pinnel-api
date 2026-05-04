package org.pinnel.pinnelapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.UserDto;
import org.pinnel.pinnelapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    // TODO(#2): replace X-Cognito-Id header with the auth interceptor's resolved principal.
    private static final String COGNITO_ID_HEADER = "X-Cognito-Id";

    private final UserService userService;

    /** GET /api/me — returns the authenticated user's profile. */
    @GetMapping
    public UserDto getCurrent(@RequestHeader(COGNITO_ID_HEADER) String cognitoId) {
        return userService.getCurrentUser(cognitoId);
    }

    /** PUT /api/me — strict-replace update of the authenticated user's editable fields (username, displayName, bio). */
    @PutMapping
    public UserDto updateCurrent(
            @RequestHeader(COGNITO_ID_HEADER) String cognitoId,
            @Valid @RequestBody UserDto update) {
        return userService.updateCurrentUser(cognitoId, update);
    }

    /** DELETE /api/me — deletes the authenticated user's account. Returns 204. Idempotent. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrent(@RequestHeader(COGNITO_ID_HEADER) String cognitoId) {
        userService.deleteCurrentUser(cognitoId);
    }
}
