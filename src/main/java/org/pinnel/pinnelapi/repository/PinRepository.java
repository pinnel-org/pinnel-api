package org.pinnel.pinnelapi.repository;

import java.util.List;
import org.pinnel.pinnelapi.entity.PinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PinRepository extends JpaRepository<PinEntity, Long> {

    @Query("""
            SELECT p FROM PinEntity p
            WHERE p.city.id = :cityId
              AND (p.user IS NULL OR p.isPublic = TRUE OR p.user.id = :userId)
            ORDER BY p.createdAt DESC
            """)
    List<PinEntity> findVisibleByCityId(@Param("cityId") Long cityId, @Param("userId") Long userId);
}
