package org.pinnel.pinnelapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.entity.TripDetailEntity;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.pinnel.pinnelapi.repository.TripDetailRepository;
import org.pinnel.pinnelapi.repository.TripRepository;
import org.pinnel.pinnelapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

    @Autowired
    private TripDetailRepository tripDetailRepository;

    @Test
    void countriesEndpointReturnsDistinctCountriesFromCallerTrips() throws Exception {
        String cognitoId = "countries-caller";
        UserEntity caller = saveUser(cognitoId, "caller@pinnel.io", "caller");

        CityEntity paris = saveCity("Test-Paris", "France");
        CityEntity nice = saveCity("Test-Nice", "France");
        CityEntity rome = saveCity("Test-Rome", "Italy");

        TripEntity trip1 = saveTrip(caller);
        TripEntity trip2 = saveTrip(caller);
        saveTripDetail(trip1, caller, paris);
        saveTripDetail(trip1, caller, nice);
        saveTripDetail(trip2, caller, rome);

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
        TripEntity otherTrip = saveTrip(other);
        saveTripDetail(otherTrip, other, saveCity("Test-Tokyo", "Japan"));

        String cognitoId = "isolated-caller";
        saveUser(cognitoId, "iso@pinnel.io", "iso");

        mvc.perform(get("/api/me/countries")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "iso@pinnel.io")
                        .header("X-Cognito-Username", "iso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    @Test
    void uploadAvatarStoresBytesAndGetReturnsThemAsImageJpeg() throws Exception {
        String cognitoId = "avatar-roundtrip";
        saveUser(cognitoId, "ava@pinnel.io", "ava");
        byte[] bytes = {1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", bytes);

        mvc.perform(multipart("/api/me/avatar")
                        .file(file)
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "ava@pinnel.io")
                        .header("X-Cognito-Username", "ava"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/me/avatar")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "ava@pinnel.io")
                        .header("X-Cognito-Username", "ava"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(bytes));
    }

    @Test
    void getAvatarReturns404WhenUserHasNoAvatar() throws Exception {
        String cognitoId = "no-avatar";
        saveUser(cognitoId, "noava@pinnel.io", "noava");

        mvc.perform(get("/api/me/avatar")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "noava@pinnel.io")
                        .header("X-Cognito-Username", "noava"))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadAvatarReturns400WhenFileIsEmpty() throws Exception {
        String cognitoId = "empty-file";
        saveUser(cognitoId, "empty@pinnel.io", "empty");
        MockMultipartFile empty = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[0]);

        mvc.perform(multipart("/api/me/avatar")
                        .file(empty)
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "empty@pinnel.io")
                        .header("X-Cognito-Username", "empty"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAvatarClearsExistingAvatarAndSubsequentGetReturns404() throws Exception {
        String cognitoId = "delete-avatar";
        saveUser(cognitoId, "del@pinnel.io", "del");
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{7, 7, 7});

        mvc.perform(multipart("/api/me/avatar")
                        .file(file)
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "del@pinnel.io")
                        .header("X-Cognito-Username", "del"))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/me/avatar")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "del@pinnel.io")
                        .header("X-Cognito-Username", "del"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/me/avatar")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "del@pinnel.io")
                        .header("X-Cognito-Username", "del"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAvatarIsIdempotentWhenUserHasNoAvatar() throws Exception {
        String cognitoId = "idempotent-delete";
        saveUser(cognitoId, "idem@pinnel.io", "idem");

        mvc.perform(delete("/api/me/avatar")
                        .header("X-Cognito-Id", cognitoId)
                        .header("X-Cognito-Email", "idem@pinnel.io")
                        .header("X-Cognito-Username", "idem"))
                .andExpect(status().isNoContent());
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

    private TripEntity saveTrip(UserEntity owner) {
        Instant now = Instant.now();
        return tripRepository.save(TripEntity.builder()
                .name("trip-" + owner.getCognitoId() + "-" + now.toEpochMilli())
                .user(owner)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private TripDetailEntity saveTripDetail(TripEntity trip, UserEntity owner, CityEntity city) {
        return tripDetailRepository.save(TripDetailEntity.builder()
                .trip(trip)
                .userId(owner.getId())
                .visitDate(LocalDate.now())
                .city(city)
                .cityOrder(0)
                .build());
    }
}
