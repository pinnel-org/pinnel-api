package org.pinnel.pinnelapi.repository;

import java.util.List;
import org.pinnel.pinnelapi.entity.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<TripEntity, Long> {

    List<TripEntity> findByUserId(Long userId);
}
