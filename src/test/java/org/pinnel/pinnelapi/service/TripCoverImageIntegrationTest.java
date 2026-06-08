package org.pinnel.pinnelapi.service;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.pinnel.pinnelapi.entity.CityEntity;
import org.pinnel.pinnelapi.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TripCoverImageIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CityRepository cityRepository;

    @Test
    void createTripReturnsCdnCoverImageUrlForFirstCity() throws Exception {
        CityEntity athens = cityRepository.save(
                CityEntity.builder().name("Athens").country("Greece").build());

        String body = """
                {
                  "name": "My Greek Trip",
                  "cityIds": [%d],
                  "pinIds": []
                }
                """.formatted(athens.getId());

        mvc.perform(post("/api/trips")
                        .header("X-Cognito-Id", "trip-cover-test-user")
                        .header("X-Cognito-Email", "tripcover@pinnel.io")
                        .header("X-Cognito-Username", "trip-cover-test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverImageUrl",
                        matchesPattern(
                                "https://d3rc6nnyfsbnsg\\.cloudfront\\.net"
                                        + "/countries/greece/cities/athens/cover_[1-5]\\.jpg")));
    }
}
