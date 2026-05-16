package org.pinnel.pinnelapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.pinnel.pinnelapi.entity.UserEntity;

public record UserDto(
        Long id,
        String cognitoId,
        String email,
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 100) String displayName,
        @NotNull @Size(max = 500) String bio,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserDto from(UserEntity user) {
        return new UserDto(
                user.getId(),
                user.getCognitoId(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
