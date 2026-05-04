package org.pinnel.pinnelapi.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String cognitoSub) {
        return UserDto.from(loadUser(cognitoSub));
    }

    @Transactional
    public UserDto updateCurrentUser(String cognitoSub, UpdateUserDto update) {
        UserEntity user = loadUser(cognitoSub);
        if (update.username() != null) user.setUsername(update.username());
        if (update.displayName() != null) user.setDisplayName(update.displayName());
        if (update.bio() != null) user.setBio(update.bio());
        return UserDto.from(user);
    }

    @Transactional
    public void deleteCurrentUser(String cognitoSub) {
        if (!userRepository.existsById(cognitoSub)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(cognitoSub);
    }

    private UserEntity loadUser(String cognitoSub) {
        return userRepository.findById(cognitoSub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
