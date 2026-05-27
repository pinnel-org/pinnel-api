package org.pinnel.pinnelapi.repository;

import java.time.Instant;
import java.util.Optional;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByCognitoId(String cognitoId);

    void deleteByCognitoId(String cognitoId);

    /** Reads the raw avatar bytes for the given user, or null if the user has no avatar (or does not exist). */
    @Query(value = "SELECT avatar FROM users WHERE id = :id", nativeQuery = true)
    byte[] findAvatarById(@Param("id") Long id);

    /** Writes the avatar bytes (or null to clear) and bumps updated_at for the given user. Returns the row count affected. */
    @Modifying
    @Query(value = "UPDATE users SET avatar = :avatar, updated_at = :updatedAt WHERE id = :id",
            nativeQuery = true)
    int updateAvatarById(@Param("id") Long id,
                         @Param("avatar") byte[] avatar,
                         @Param("updatedAt") Instant updatedAt);
}
