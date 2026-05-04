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

    public UserDto getCurrentUser(String cognitoSub) {
        UserEntity user = userRepository.findById(cognitoSub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateCurrentUser(String cognitoSub, UserDto update) {
        UserEntity user = userRepository.findById(cognitoSub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setUsername(update.username());
        user.setDisplayName(update.displayName());
        user.setBio(update.bio());
        user.setUpdatedAt(Instant.now());
        return UserDto.from(user);
    }

    public void deleteCurrentUser(String cognitoSub) {
        userRepository.deleteById(cognitoSub);
    }
}
