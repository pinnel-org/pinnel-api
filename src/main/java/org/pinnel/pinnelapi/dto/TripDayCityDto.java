package org.pinnel.pinnelapi.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.pinnel.pinnelapi.entity.TripDayCityEntity;

public record TripDayCityDto(
        @NotNull Long cityId,
        @NotNull List<Long> pinIds
) {
    public static TripDayCityDto from(TripDayCityEntity entity) {
        return new TripDayCityDto(
                entity.getCity().getId(),
                entity.getPins().stream()
                        .map(p -> p.getPin().getId())
                        .toList()
        );
    }
}
