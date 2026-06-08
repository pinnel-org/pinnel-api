package org.pinnel.pinnelapi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.pinnel.pinnelapi.entity.TripDetailPinEntity;

public record TripDetailPinDto(
        Long id,
        Long tripDetailId,
        Long userId,
        @NotNull Long pinId,
        Integer pinOrder,
        @JsonFormat(pattern = "HH:mm") LocalTime visitTime,
        @PositiveOrZero BigDecimal budget
) {
    public static TripDetailPinDto from(TripDetailPinEntity e) {
        return new TripDetailPinDto(
                e.getId(),
                e.getTripDetail().getId(),
                e.getUserId(),
                e.getPin().getId(),
                e.getPinOrder(),
                e.getVisitTime(),
                e.getBudget()
        );
    }
}
