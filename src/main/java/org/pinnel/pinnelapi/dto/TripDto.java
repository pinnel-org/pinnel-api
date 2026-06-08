package org.pinnel.pinnelapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import org.pinnel.pinnelapi.entity.TripEntity;

public record TripDto(
        Long id,
        @NotBlank @Size(max = 120) String name,
        @DecimalMin("0") BigDecimal budget,
        Long userId,
        String coverImageUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public static TripDto from(TripEntity trip) {
        return new TripDto(
                trip.getId(),
                trip.getName(),
                trip.getBudget(),
                trip.getUser().getId(),
                trip.getCoverImageUrl(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }
}
