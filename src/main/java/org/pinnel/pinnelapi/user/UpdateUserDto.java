package org.pinnel.pinnelapi.user;

public record UpdateUserDto(
        String username,
        String displayName,
        String bio
) {}
