package org.pinnel.pinnelapi.dto;

import java.math.BigDecimal;
import org.pinnel.pinnelapi.entity.CityEntity;

public record CityDto(
        Long id,
        String name,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer population
) {
    public static CityDto from(CityEntity city) {
        return new CityDto(
                city.getId(),
                city.getName(),
                city.getCountry(),
                city.getLatitude(),
                city.getLongitude(),
                city.getPopulation()
        );
    }
}
