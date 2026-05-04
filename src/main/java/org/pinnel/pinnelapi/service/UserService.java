package org.pinnel.pinnelapi.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.UserDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** Returns the current user's profile. Throws 404 if no user with the given Cognito id exists. */
    public UserDto getCurrentUser(String cognitoId) {
        return UserDto.from(getUser(cognitoId));
    }

    /** Applies the editable fields (username, displayName, bio) from the request to the current user and bumps updatedAt. Throws 404 if the user does not exist. */
    @Transactional
    public UserDto updateCurrentUser(String cognitoId, UserDto update) {
        UserEntity user = getUser(cognitoId);
        user.setUsername(update.username());
        user.setDisplayName(update.displayName());
        user.setBio(update.bio());
        user.setUpdatedAt(Instant.now());
        return UserDto.from(user);
    }

    /** Deletes the current user. Idempotent — does nothing if the user does not exist. */
    @Transactional
    public void deleteCurrentUser(String cognitoId) {
        userRepository.deleteByCognitoId(cognitoId);
    }

    private UserEntity getUser(String cognitoId) {
        return userRepository.findByCognitoId(cognitoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
