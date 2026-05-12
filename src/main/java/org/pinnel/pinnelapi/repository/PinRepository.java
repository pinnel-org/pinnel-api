package org.pinnel.pinnelapi.repository;

import java.util.List;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinRepository extends JpaRepository<PinEntity, Long> {

    List<PinEntity> findByCityId(Long cityId);

    List<PinEntity> findByUserId(Long userId);

    List<PinEntity> findByIsPublicTrue();
}
