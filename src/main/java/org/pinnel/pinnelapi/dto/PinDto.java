package org.pinnel.pinnelapi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import org.pinnel.pinnelapi.entity.PinEntity;

public record PinDto(
        Long id,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String overview,
        @Size(max = 2000) String visitorTips,
        @Size(max = 2000) String history,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotNull Boolean isPublic,
        @NotNull Long cityId,
        Long userId,
        Instant createdAt,
        Instant updatedAt,
        String logoUrlSmall,
        String logoUrlBig
) {
    public static PinDto from(PinEntity pin, String logoUrlSmall, String logoUrlBig) {
        return new PinDto(
                pin.getId(),
                pin.getName(),
                pin.getOverview(),
                pin.getVisitorTips(),
                pin.getHistory(),
                pin.getLatitude(),
                pin.getLongitude(),
                pin.isPublic(),
                pin.getCity().getId(),
                pin.getUser() != null ? pin.getUser().getId() : null,
                pin.getCreatedAt(),
                pin.getUpdatedAt(),
                logoUrlSmall,
                logoUrlBig
        );
    }
}
