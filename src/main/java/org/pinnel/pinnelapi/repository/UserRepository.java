package org.pinnel.pinnelapi.repository;

import java.util.Optional;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByCognitoId(String cognitoId);

    void deleteByCognitoId(String cognitoId);
}
