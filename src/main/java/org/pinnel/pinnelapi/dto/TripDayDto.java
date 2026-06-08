package org.pinnel.pinnelapi.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.pinnel.pinnelapi.entity.TripDayEntity;

public record TripDayDto(
        @NotNull LocalDate visitDate,
        @NotNull List<TripDayCityDto> cities
) {
    public static TripDayDto from(TripDayEntity entity) {
        return new TripDayDto(
                entity.getVisitDate(),
                entity.getCities().stream()
                        .map(TripDayCityDto::from)
                        .toList()
        );
    }
}
