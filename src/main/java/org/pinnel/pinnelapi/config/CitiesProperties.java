package org.pinnel.pinnelapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tunable settings for the cities endpoints. Configurable via {@code pinnel.cities.*}. */
@ConfigurationProperties(prefix = "pinnel.cities")
@Getter
@Setter
public class CitiesProperties {

    /** Max number of results returned by {@code GET /api/cities?search=...}. */
    private int searchLimit = 20;
}
