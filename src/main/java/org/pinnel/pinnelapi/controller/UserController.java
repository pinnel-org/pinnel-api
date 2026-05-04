package org.pinnel.pinnelapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.UpdateUserDto;
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

    // TODO(#2): replace X-Cognito-Sub header with the auth interceptor's resolved principal.
    private static final String COGNITO_SUB_HEADER = "X-Cognito-Sub";

    private final UserService userService;

    @GetMapping
    public UserDto getCurrent(@RequestHeader(COGNITO_SUB_HEADER) String cognitoSub) {
        return userService.getCurrentUser(cognitoSub);
    }

    @PutMapping
    public UserDto updateCurrent(
            @RequestHeader(COGNITO_SUB_HEADER) String cognitoSub,
            @Valid @RequestBody UpdateUserDto update) {
        return userService.updateCurrentUser(cognitoSub, update);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrent(@RequestHeader(COGNITO_SUB_HEADER) String cognitoSub) {
        userService.deleteCurrentUser(cognitoSub);
    }
}
