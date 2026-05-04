package org.pinnel.pinnelapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pinnel.pinnelapi.dto.UserDto;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String COGNITO_ID = "cognito-123";
    private static final Long USER_ID = 42L;
    private static final Instant ORIGINAL_TIMESTAMP = Instant.parse("2026-05-01T00:00:00Z");

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity existing;

    @BeforeEach
    void setUp() {
        existing = UserEntity.builder()
                .id(USER_ID)
                .cognitoId(COGNITO_ID)
                .email("alice@pinnel.io")
                .username("alice")
                .displayName("Alice")
                .bio("hi")
                .createdAt(ORIGINAL_TIMESTAMP)
                .updatedAt(ORIGINAL_TIMESTAMP)
                .build();
    }

    @Test
    void getCurrentUserReturnsDto() {
        given(userRepository.findByCognitoId(COGNITO_ID)).willReturn(Optional.of(existing));

        UserDto result = userService.getCurrentUser(COGNITO_ID);

        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.cognitoId()).isEqualTo(COGNITO_ID);
        assertThat(result.email()).isEqualTo("alice@pinnel.io");
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.displayName()).isEqualTo("Alice");
        assertThat(result.bio()).isEqualTo("hi");
        assertThat(result.createdAt()).isEqualTo(ORIGINAL_TIMESTAMP);
        assertThat(result.updatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
    }

    @Test
    void getCurrentUserThrowsNotFoundWhenMissing() {
        given(userRepository.findByCognitoId(COGNITO_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(COGNITO_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateCurrentUserAppliesEditableFieldsAndBumpsUpdatedAt() {
        given(userRepository.findByCognitoId(COGNITO_ID)).willReturn(Optional.of(existing));

        UserDto update = new UserDto(
                null, null, null,
                "alice2", "Alice 2.0", "new bio",
                null, null
        );

        UserDto result = userService.updateCurrentUser(COGNITO_ID, update);

        assertThat(existing.getUsername()).isEqualTo("alice2");
        assertThat(existing.getDisplayName()).isEqualTo("Alice 2.0");
        assertThat(existing.getBio()).isEqualTo("new bio");
        assertThat(existing.getUpdatedAt()).isAfter(ORIGINAL_TIMESTAMP);

        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.username()).isEqualTo("alice2");
        assertThat(result.displayName()).isEqualTo("Alice 2.0");
        assertThat(result.bio()).isEqualTo("new bio");
        assertThat(result.updatedAt()).isAfter(ORIGINAL_TIMESTAMP);
    }

    @Test
    void updateCurrentUserIgnoresIdentityAndTimestampFieldsFromBody() {
        given(userRepository.findByCognitoId(COGNITO_ID)).willReturn(Optional.of(existing));

        UserDto malicious = new UserDto(
                999L,
                "different-cognito-id",
                "attacker@evil.io",
                "alice", "Alice", "hi",
                Instant.parse("1999-01-01T00:00:00Z"),
                Instant.parse("1999-01-01T00:00:00Z")
        );

        userService.updateCurrentUser(COGNITO_ID, malicious);

        assertThat(existing.getId()).isEqualTo(USER_ID);
        assertThat(existing.getCognitoId()).isEqualTo(COGNITO_ID);
        assertThat(existing.getEmail()).isEqualTo("alice@pinnel.io");
        assertThat(existing.getCreatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
    }

    @Test
    void updateCurrentUserThrowsNotFoundWhenMissing() {
        given(userRepository.findByCognitoId(COGNITO_ID)).willReturn(Optional.empty());
        UserDto update = new UserDto(null, null, null, "x", "X", "x", null, null);

        assertThatThrownBy(() -> userService.updateCurrentUser(COGNITO_ID, update))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteCurrentUserDelegatesWithoutPreCheck() {
        userService.deleteCurrentUser(COGNITO_ID);

        verify(userRepository).deleteByCognitoId(COGNITO_ID);
        verify(userRepository, never()).existsById(any());
    }
}
