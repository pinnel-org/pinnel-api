package org.pinnel.pinnelapi.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.pinnel.pinnelapi.entity.TripDetailEntity;

public record TripDetailDto(
        Long id,
        Long tripId,
        Long userId,
        @NotNull LocalDate visitDate,
        @NotNull Long cityId,
        Integer cityOrder
) {
    public static TripDetailDto from(TripDetailEntity e) {
        return new TripDetailDto(
                e.getId(),
                e.getTrip().getId(),
                e.getUserId(),
                e.getVisitDate(),
                e.getCity().getId(),
                e.getCityOrder()
        );
    }
}
