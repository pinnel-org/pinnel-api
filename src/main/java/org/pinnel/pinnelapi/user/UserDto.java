package org.pinnel.pinnelapi.user;

import java.time.Instant;

public record UserDto(
        String cognitoSub,
        String email,
        String username,
        String displayName,
        String bio,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserDto from(UserEntity user) {
        return new UserDto(
                user.getCognitoSub(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
