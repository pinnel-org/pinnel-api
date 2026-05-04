package org.pinnel.pinnelapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    void findOrCreateReturnsExistingWhenFound() {
        given(userRepository.findByCognitoId(COGNITO_ID)).willReturn(Optional.of(existing));

        UserEntity result = userService.findOrCreateByCognitoId(COGNITO_ID, "ignored@pinnel.io", "ignored");

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateCreatesNewUserWhenNotFound() {
        given(userRepository.findByCognitoId(COGNITO_ID)).willReturn(Optional.empty());
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        UserEntity result = userService.findOrCreateByCognitoId(COGNITO_ID, "new@pinnel.io", "newuser");

        assertThat(result.getCognitoId()).isEqualTo(COGNITO_ID);
        assertThat(result.getEmail()).isEqualTo("new@pinnel.io");
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void getCurrentUserMapsEntityToDtoWithoutTouchingRepository() {
        UserDto result = userService.getCurrentUser(existing);

        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.cognitoId()).isEqualTo(COGNITO_ID);
        assertThat(result.email()).isEqualTo("alice@pinnel.io");
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.displayName()).isEqualTo("Alice");
        assertThat(result.bio()).isEqualTo("hi");
        assertThat(result.createdAt()).isEqualTo(ORIGINAL_TIMESTAMP);
        assertThat(result.updatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateCurrentUserAppliesEditableFieldsAndBumpsUpdatedAt() {
        given(userRepository.save(existing)).willAnswer(inv -> inv.getArgument(0));

        UserDto update = new UserDto(
                null, null, null,
                "alice2", "Alice 2.0", "new bio",
                null, null
        );

        UserDto result = userService.updateCurrentUser(existing, update);

        assertThat(existing.getUsername()).isEqualTo("alice2");
        assertThat(existing.getDisplayName()).isEqualTo("Alice 2.0");
        assertThat(existing.getBio()).isEqualTo("new bio");
        assertThat(existing.getUpdatedAt()).isAfter(ORIGINAL_TIMESTAMP);

        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.username()).isEqualTo("alice2");
        assertThat(result.updatedAt()).isAfter(ORIGINAL_TIMESTAMP);
    }

    @Test
    void updateCurrentUserIgnoresIdentityAndTimestampFieldsFromBody() {
        given(userRepository.save(existing)).willAnswer(inv -> inv.getArgument(0));

        UserDto malicious = new UserDto(
                999L,
                "different-cognito-id",
                "attacker@evil.io",
                "alice", "Alice", "hi",
                Instant.parse("1999-01-01T00:00:00Z"),
                Instant.parse("1999-01-01T00:00:00Z")
        );

        userService.updateCurrentUser(existing, malicious);

        assertThat(existing.getId()).isEqualTo(USER_ID);
        assertThat(existing.getCognitoId()).isEqualTo(COGNITO_ID);
        assertThat(existing.getEmail()).isEqualTo("alice@pinnel.io");
        assertThat(existing.getCreatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
    }

    @Test
    void deleteCurrentUserCallsDeleteByIdWithoutPreCheck() {
        userService.deleteCurrentUser(existing);

        verify(userRepository).deleteById(USER_ID);
        verify(userRepository, never()).existsById(any());
    }
}
