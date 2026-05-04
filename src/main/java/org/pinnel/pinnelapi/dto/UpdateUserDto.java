package org.pinnel.pinnelapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserDto(
        @NotBlank @Size(max = 50) String username,
        @NotNull @Size(max = 100) String displayName,
        @NotNull @Size(max = 500) String bio
) {}
