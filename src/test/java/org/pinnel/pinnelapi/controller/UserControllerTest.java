package org.pinnel.pinnelapi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.pinnel.pinnelapi.repository.TripRepository;
import org.pinnel.pinnelapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private CityRepository cityRepository;

    @Test
    void countriesEndpointReturnsDistinctCountriesFromCallerTrips() throws Exception {
        String cognitoId = "countries-caller";
        UserEntity caller = saveUser(cognitoId, "caller@pinnel.io", "caller");

        CityEntity paris = saveCity("Paris", "France");
        CityEntity nice = saveCity("Nice", "France");
        CityEntity rome = saveCity("Rome", "Italy");

        saveTrip(caller, Set.of(paris, nice));
        saveTrip(caller, Set.of(rome));

        mvc.perform(get("/api/me/countries")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "caller@pinnel.io")
                        .header("X-Cognito-Username", "caller"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", Matchers.containsInAnyOrder("France", "Italy")));
    }

    @Test
    void countriesEndpointReturnsEmptyArrayWhenCallerHasNoTrips() throws Exception {
        String cognitoId = "no-trips-caller";
        saveUser(cognitoId, "empty@pinnel.io", "empty");

        mvc.perform(get("/api/me/countries")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "empty@pinnel.io")
                        .header("X-Cognito-Username", "empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    @Test
    void countriesEndpointDoesNotLeakCountriesFromOtherUsersTrips() throws Exception {
        String otherCognito = "other-user";
        UserEntity other = saveUser(otherCognito, "other@pinnel.io", "other");
        saveTrip(other, Set.of(saveCity("Tokyo", "Japan")));

        String cognitoId = "isolated-caller";
        saveUser(cognitoId, "iso@pinnel.io", "iso");

        mvc.perform(get("/api/me/countries")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "iso@pinnel.io")
                        .header("X-Cognito-Username", "iso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    private UserEntity saveUser(String cognitoId, String email, String username) {
        Instant now = Instant.now();
        return userRepository.save(UserEntity.builder()
                .cognitoId(cognitoId)
                .email(email)
                .username(username)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private CityEntity saveCity(String name, String country) {
        return cityRepository.save(CityEntity.builder()
                .name(name)
                .country(country)
                .build());
    }

    private TripEntity saveTrip(UserEntity owner, Set<CityEntity> cities) {
        Instant now = Instant.now();
        return tripRepository.save(TripEntity.builder()
                .name("trip-" + owner.getCognitoId() + "-" + now.toEpochMilli())
                .user(owner)
                .createdAt(now)
                .updatedAt(now)
                .cities(cities)
                .build());
    }
}
