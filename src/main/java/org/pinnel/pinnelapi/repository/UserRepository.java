package org.pinnel.pinnelapi.repository;

import org.pinnel.pinnelapi.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
}
