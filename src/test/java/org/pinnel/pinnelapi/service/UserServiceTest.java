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

    private static final String SUB = "sub-123";
    private static final Instant ORIGINAL_TIMESTAMP = Instant.parse("2026-05-01T00:00:00Z");

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity existing;

    @BeforeEach
    void setUp() {
        existing = UserEntity.builder()
                .cognitoSub(SUB)
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
        given(userRepository.findById(SUB)).willReturn(Optional.of(existing));

        UserDto result = userService.getCurrentUser(SUB);

        assertThat(result.cognitoSub()).isEqualTo(SUB);
        assertThat(result.email()).isEqualTo("alice@pinnel.io");
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.displayName()).isEqualTo("Alice");
        assertThat(result.bio()).isEqualTo("hi");
        assertThat(result.createdAt()).isEqualTo(ORIGINAL_TIMESTAMP);
        assertThat(result.updatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
    }

    @Test
    void getCurrentUserThrowsNotFoundWhenMissing() {
        given(userRepository.findById(SUB)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(SUB))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateCurrentUserAppliesEditableFieldsAndBumpsUpdatedAt() {
        given(userRepository.findById(SUB)).willReturn(Optional.of(existing));

        UserDto update = new UserDto(
                null, null,
                "alice2", "Alice 2.0", "new bio",
                null, null
        );

        UserDto result = userService.updateCurrentUser(SUB, update);

        assertThat(existing.getUsername()).isEqualTo("alice2");
        assertThat(existing.getDisplayName()).isEqualTo("Alice 2.0");
        assertThat(existing.getBio()).isEqualTo("new bio");
        assertThat(existing.getUpdatedAt()).isAfter(ORIGINAL_TIMESTAMP);

        assertThat(result.username()).isEqualTo("alice2");
        assertThat(result.displayName()).isEqualTo("Alice 2.0");
        assertThat(result.bio()).isEqualTo("new bio");
        assertThat(result.updatedAt()).isAfter(ORIGINAL_TIMESTAMP);
    }

    @Test
    void updateCurrentUserIgnoresIdentityAndTimestampFieldsFromBody() {
        given(userRepository.findById(SUB)).willReturn(Optional.of(existing));

        UserDto malicious = new UserDto(
                "different-sub",
                "attacker@evil.io",
                "alice", "Alice", "hi",
                Instant.parse("1999-01-01T00:00:00Z"),
                Instant.parse("1999-01-01T00:00:00Z")
        );

        userService.updateCurrentUser(SUB, malicious);

        assertThat(existing.getCognitoSub()).isEqualTo(SUB);
        assertThat(existing.getEmail()).isEqualTo("alice@pinnel.io");
        assertThat(existing.getCreatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
    }

    @Test
    void updateCurrentUserThrowsNotFoundWhenMissing() {
        given(userRepository.findById(SUB)).willReturn(Optional.empty());
        UserDto update = new UserDto(null, null, "x", "X", "x", null, null);

        assertThatThrownBy(() -> userService.updateCurrentUser(SUB, update))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteCurrentUserDelegatesWithoutPreCheck() {
        userService.deleteCurrentUser(SUB);

        verify(userRepository).deleteById(SUB);
        verify(userRepository, never()).existsById(any());
    }
}
