package org.pinnel.pinnelapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.pinnel.pinnelapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CognitoAuthFilterIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void firstRequestProvisionsUserAndSubsequentRequestsReuseTheSameRow() throws Exception {
        mvc.perform(get("/api/me")
                        .header("X-Cognito-Id", "new-cognito-id")
                        .header("X-Cognito-Email", "new@pinnel.io")
                        .header("X-Cognito-Username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cognitoId").value("new-cognito-id"))
                .andExpect(jsonPath("$.email").value("new@pinnel.io"))
                .andExpect(jsonPath("$.username").value("newuser"));

        assertThat(userRepository.findByCognitoId("new-cognito-id")).isPresent();

        mvc.perform(get("/api/me")
                        .header("X-Cognito-Id", "new-cognito-id")
                        .header("X-Cognito-Email", "different@pinnel.io")
                        .header("X-Cognito-Username", "different"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@pinnel.io"))
                .andExpect(jsonPath("$.username").value("newuser"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void requestWithoutCognitoIdHeaderReturns401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRemovesTheUser() throws Exception {
        mvc.perform(get("/api/me")
                        .header("X-Cognito-Id", "to-be-deleted")
                        .header("X-Cognito-Email", "del@pinnel.io")
                        .header("X-Cognito-Username", "del"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/me")
                        .header("X-Cognito-Id", "to-be-deleted")
                        .header("X-Cognito-Email", "del@pinnel.io")
                        .header("X-Cognito-Username", "del"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByCognitoId("to-be-deleted")).isEmpty();
    }
}
