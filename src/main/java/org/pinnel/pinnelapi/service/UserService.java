package org.pinnel.pinnelapi.service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.dto.UserDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.TripRepository;
import org.pinnel.pinnelapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    /** Returns the existing user for the given Cognito id, or just-in-time creates one populated from the supplied email and username. */
    @Transactional
    public UserEntity findOrCreateByCognitoId(String cognitoId, String email, String username) {
        return userRepository.findByCognitoId(cognitoId)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return userRepository.save(UserEntity.builder()
                            .cognitoId(cognitoId)
                            .email(email)
                            .username(username)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                });
    }

    /** Returns the current user's profile. */
    public UserDto getCurrentUser(UserEntity user) {
        return UserDto.from(user);
    }

    /** Applies the editable fields (username, displayName, bio) from the request to the current user and bumps updatedAt. */
    @Transactional
    public UserDto updateCurrentUser(UserEntity user, UserDto update) {
        user.setUsername(update.username());
        user.setDisplayName(update.displayName());
        user.setBio(update.bio());
        user.setUpdatedAt(Instant.now());
        return UserDto.from(userRepository.save(user));
    }

    /** Deletes the current user. Idempotent — does nothing if the user has already been deleted. */
    @Transactional
    public void deleteCurrentUser(UserEntity user) {
        userRepository.deleteById(user.getId());
    }

    /** Returns the distinct set of country names appearing across all cities of the caller's trips. */
    public Set<String> listMyCountries(UserEntity user) {
        return tripRepository.findDistinctCountriesByUserId(user.getId());
    }

    /** Returns the current user's avatar bytes, or empty if no avatar is set. */
    public Optional<byte[]> getCurrentUserAvatar(UserEntity user) {
        return Optional.ofNullable(userRepository.findAvatarById(user.getId()));
    }

    /** Replaces the current user's avatar with the given bytes and bumps updatedAt. */
    @Transactional
    public void saveCurrentUserAvatar(UserEntity user, byte[] avatar) {
        userRepository.updateAvatarById(user.getId(), avatar, Instant.now());
    }

    /** Clears the current user's avatar (sets it to null) and bumps updatedAt. Idempotent. */
    @Transactional
    public void deleteCurrentUserAvatar(UserEntity user) {
        userRepository.updateAvatarById(user.getId(), null, Instant.now());
    }
}
